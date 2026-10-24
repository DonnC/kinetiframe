package com.donnclab.kinetiframe.export.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.audio.StreamingAudioMix
import com.donnclab.kinetiframe.core.document.MAX_AUDIO_TRACKS
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.db.ExportDao
import com.donnclab.kinetiframe.data.db.ExportEntity
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.render.ProjectFrameCompositor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Renders one project's whole timeline to a single MP4 file -- the "ultimate verification," where
 * everything that went into building the project (background, layers, packs already stamped into
 * layer pixels, actors, performances, and up to [MAX_AUDIO_TRACKS]
 * audio lanes and embedded scene video) shows up in one flattened, shareable video.
 * Legacy tracing clips are excluded; embedded videos contribute pictures, not source audio.
 *
 * The project's own [StageDimensions] and FPS (chosen once, at
 * project creation) still fully determine how the timeline is *rendered* -- there's no separate
 * "working resolution" picker. What's chosen at export time is only a destination [ExportPreset]:
 * when it names a platform target, composited frames are letterboxed to that target resolution
 * (see the scaling step below) without the project's own canvas or FPS ever changing. A null/
 * "same as project" preset skips scaling entirely and encodes at the project's own stage size.
 *
 * Audio is mixed in bounded windows and AAC-spooled before video starts. Both formats are
 * registered before starting the final muxer; audio packets are transferred after video drains.
 *
 * Every allocation-heavy step (the compositor's reused bitmaps, decoding the background once,
 * fully-synchronous audio encoding rather than an async pipeline) is a deliberate mobile-first
 * optimization: this runs on a phone, not a render farm, and a multi-hundred-frame export must
 * not thrash the GC or hold multiple copies of the same frame in memory at once.
 */
class ProjectExporter(
    private val context: Context,
    private val actorStore: DocumentFileStore,
    private val actorRepository: ActorRepository,
    private val performanceRepository: PerformanceRepository,
    private val projectRepository: ProjectRepository,
    private val exportDao: ExportDao? = null,
) {
    /** Renders [projectId] to a private MP4 under `filesDir/video-exports/`, reporting progress
     *  in `[0f, 1f]`. The returned file is app-private -- callers use [saveToGallery] or [shareVideo]
     *  to actually get it in front of the user, mirroring the existing `.kframe`/`.kpack` FileProvider
     *  pattern rather than writing straight into MediaStore mid-encode. */
    suspend fun export(
        projectId: String,
        targetDimensions: StageDimensions? = null,
        onProgress: (Float) -> Unit = {},
    ): StoreResult<File> = runCatching {
        val (manifest, document) = when (val result = projectRepository.load(projectId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
        val project = document.project
        require(document.totalFrames > 0) { "Project has no frames to export" }
        val outputDimensions = targetDimensions ?: project.stage
        val needsLetterbox = outputDimensions != project.stage

        val backgroundBitmap = project.backgroundAssetId
            ?.let { project.assets[it]?.sourcePath }
            ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        require(project.backgroundAssetId == null || backgroundBitmap != null) {
            "Saved project background is missing or unreadable"
        }

        val cacheScope = CoroutineScope(SupervisorJob())
        val cacheManager = FrameCacheManager(File(actorStore.projectDirectory(projectId), "frames"), cacheScope).apply {
            restoreExposures(document.linkedCels, document.totalFrames)
        }
        val actorRenderer = if (project.actorPlacements.isEmpty()) {
            null
        } else {
            ActorFrameRenderer(actorRepository, performanceRepository, actorStore)
        }
        val fonts = if (project.nodes.isEmpty()) {
            null
        } else {
            FontRegistry(
                context,
            )
        }
        val compositor =
            ProjectFrameCompositor(project, document.frameLayers, cacheManager, actorRenderer, backgroundBitmap, fonts)

        val outputDirectory = File(context.filesDir, "video-exports").apply { mkdirs() }
        val outputFile = File(outputDirectory, "$projectId-${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        var muxerStarted = false
        var audioResolved = project.audioClips.isEmpty()
        var videoResolved = false
        var audioFile: File? = null
        var pcmFile: File? = null
        var audioExtractor: MediaExtractor? = null
        var audioTrack = -1
        var videoEncoder: VideoEncoder? = null
        var letterboxScratch: Bitmap? = null
        var completed = false
        fun tryStartMuxer() {
            if (!muxerStarted && audioResolved && videoResolved) {
                muxer.start()
                muxerStarted = true
            }
        }

        try {
            // A muted lane is silent in the export too -- muting is a mixing decision, so it has
            // to hold at render time, not only in the editor's monitoring path.
            val audible = project.audioClips.values.filterNot { clip ->
                project.audioTracks[clip.trackIndex]?.isMuted == true
            }
            val pcm = File.createTempFile("mix-export-", ".pcm", context.cacheDir)
            pcmFile = pcm
            val mix = StreamingAudioMix().mix(audible, project.assets, project.fps, document.totalFrames, pcm)
            if (mix != null) {
                // Spool AAC to disk so samples never reach an unstarted two-track muxer.
                val spool = File.createTempFile("audio-export-", ".mp4", context.cacheDir)
                audioFile = spool
                val audioMuxer = MediaMuxer(spool.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var audioStarted = false
                try {
                    AudioEncoder(audioMuxer).encodeAndMux(mix) {
                        audioMuxer.start()
                        audioStarted = true
                    }
                    check(audioStarted) { "Audio encoder did not produce a track" }
                    audioMuxer.stop()
                } finally {
                    audioMuxer.release()
                }
                val extractor = MediaExtractor()
                audioExtractor = extractor
                extractor.setDataSource(spool.path)
                require(extractor.trackCount == 1) { "Invalid encoded audio track" }
                audioTrack = muxer.addTrack(extractor.getTrackFormat(0))
                extractor.selectTrack(0)
                audioResolved = true
                tryStartMuxer()
            } else {
                audioResolved = true
            }

            val encoder = VideoEncoder(muxer, outputDimensions.width, outputDimensions.height, project.fps)
            videoEncoder = encoder
            encoder.start()
            // Allocated once, outside the loop, and reused for every frame -- same reasoning as
            // ProjectFrameCompositor's own reused bitmaps: a multi-hundred-frame export must not
            // allocate a fresh bitmap per frame just to letterbox it.
            letterboxScratch =
                if (needsLetterbox) {
                    Bitmap.createBitmap(
                        outputDimensions.width,
                        outputDimensions.height,
                        Bitmap.Config.ARGB_8888,
                    )
                } else {
                    null
                }
            val letterboxCanvas = letterboxScratch?.let { Canvas(it) }
            val letterboxPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val totalFrames = document.totalFrames
            for (frame in 0 until totalFrames) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val composed = compositor.composite(frame)
                val bitmap = if (letterboxScratch != null && letterboxCanvas != null) {
                    // Preserve the project's own aspect ratio inside the chosen platform target
                    // rather than stretching it -- black bars, not distortion, exactly like
                    // VideoEncoder's own opaque backdrop for undrawn pixels.
                    letterboxCanvas.drawColor(Color.BLACK)
                    val scale = minOf(
                        outputDimensions.width.toFloat() / composed.width,
                        outputDimensions.height.toFloat() / composed.height,
                    )
                    val drawWidth = composed.width * scale
                    val drawHeight = composed.height * scale
                    val left = (outputDimensions.width - drawWidth) / 2f
                    val top = (outputDimensions.height - drawHeight) / 2f
                    letterboxCanvas.drawBitmap(
                        composed,
                        null,
                        RectF(left, top, left + drawWidth, top + drawHeight),
                        letterboxPaint,
                    )
                    letterboxScratch
                } else {
                    composed
                }
                val ptsUs = ExportTimeline.timeUs(frame.toLong(), project.fps)
                encoder.encodeFrame(bitmap, ptsUs) {
                    videoResolved = true
                    tryStartMuxer()
                }
                onProgress((frame + 1).toFloat() / totalFrames.toFloat())
            }
            val lastPtsUs = ExportTimeline.timeUs(totalFrames.toLong(), project.fps)
            encoder.signalEndOfStreamAndDrain(lastPtsUs) {
                videoResolved = true
                tryStartMuxer()
            }
            audioExtractor?.let { extractor ->
                val packet = ByteBuffer.allocateDirect(256 * 1024)
                val info = MediaCodec.BufferInfo()
                while (true) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    packet.clear()
                    val size = extractor.readSampleData(packet, 0)
                    if (size < 0) break
                    info.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                    muxer.writeSampleData(audioTrack, packet, info)
                    extractor.advance()
                }
            }
            check(muxerStarted) { "Encoder did not produce a video track" }
            muxer.stop()
            completed = true
        } finally {
            videoEncoder?.release()
            letterboxScratch?.recycle()
            audioExtractor?.release()
            audioFile?.delete()
            pcmFile?.delete()
            muxer.release()
            if (!completed) outputFile.delete()
            compositor.release()
            // Export only ever reads frames (getFrame), never writes -- cancelling the scope here is
            // just hygiene for FrameCacheManager's own internal coroutines, never touches a single
            // persisted frame file. Deliberately NOT calling cacheManager.clear() -- that wipes the
            // project's actual frame pixels from disk, which is exactly the opposite of what an
            // export should ever do to a project.
            cacheScope.cancel()
        }

        exportDao?.upsert(
            ExportEntity(
                exportId = "export-${UUID.randomUUID()}",
                projectId = projectId,
                filePath = outputFile.absolutePath,
                createdAtEpochMs = System.currentTimeMillis(),
                durationFrames = document.totalFrames,
                fps = project.fps,
                widthPx = outputDimensions.width,
                heightPx = outputDimensions.height,
                fileSizeBytes = outputFile.length(),
            ),
        )
        outputFile
    }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Export failed", it) })

    /** Copies an already-exported MP4 into the user's public gallery via MediaStore, so it
     *  survives an app uninstall and shows up in their normal camera-roll/gallery apps -- Android's
     *  Scoped Storage rules mean this app can never write directly into `Movies/` itself. */
    fun saveToGallery(file: File, displayName: String): StoreResult<Unit> = runCatching {
        require(file.isFile && file.length() > 0) { "Exported video is missing" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/KinetiFrame")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create a gallery entry")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not write to the gallery entry")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Could not save to gallery", it) })

    /** Hands an already-exported MP4 to the system share sheet via a FileProvider content:// URI
     *  -- the exact same pattern the home screen's project and pack share
     *  helpers already use for `.kframe`/`.kpack` files, so exporting doesn't invent a second
     *  sharing convention. */
    fun shareVideo(file: File, displayName: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share $displayName"))
    }
}
