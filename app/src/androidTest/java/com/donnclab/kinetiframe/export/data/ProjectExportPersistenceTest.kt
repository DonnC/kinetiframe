package com.donnclab.kinetiframe.export.data

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.media.audio.AudioMixer
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Hardware acceptance tests; do not substitute JVM codec stubs for these assertions. */
@RunWith(AndroidJUnit4::class)
class ProjectExportPersistenceTest {
    @Test fun savedCanvasAndFpsAreUsedByVideoOnlyExport() = checkExport(withAudio = false)

    @Test fun audioAndVideoShareAValidMuxerAndTimeline() = checkExport(withAudio = true)

    private fun checkExport(withAudio: Boolean) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = File(context.filesDir, "persistence-test-${UUID.randomUUID()}").apply { mkdirs() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var videoFile: File? = null
        try {
            val files = DocumentFileStore(root)
            val id = "project-test"
            val stage = StageDimensions(160, 128)
            val layer = Layer(name = "Main", id = "main")
            val cache = FrameCacheManager(File(files.projectDirectory(id), "frames"), scope)
            repeat(12) { frame ->
                cache.putFrame(
                    "main",
                    frame,
                    Bitmap.createBitmap(160, 128, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(if (frame < 6) Color.RED else Color.BLUE)
                    },
                )
            }
            cache.persistAllToDisk(listOf("main"), 12)
            val sound = File(files.projectDirectory(id), "source.mp4")
            if (withAudio) {
                val muxer = MediaMuxer(sound.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                try {
                    val samples = ShortArray(AudioMixer.SAMPLE_RATE_HZ * 2) { index ->
                        (
                            kotlin.math.sin(
                                index / 2.0 * 2 * Math.PI * 440 / AudioMixer.SAMPLE_RATE_HZ,
                            ) * 3000
                            ).toInt().toShort()
                    }
                    AudioEncoder(muxer).encodeAndMux(AudioMixer.MixResult(samples, AudioMixer.SAMPLE_RATE_HZ)) {
                        muxer.start()
                    }
                    muxer.stop()
                } finally {
                    muxer.release()
                }
            }
            val project = ProjectDocument(
                id = id,
                stage = stage,
                fps = 12,
                layers = listOf(layer),
                assets = if (withAudio) {
                    mapOf(
                        "sound" to ProjectAsset("sound", AssetKind.AUDIO, sound.path, 1, 1),
                    )
                } else {
                    emptyMap()
                },
                audioClips = if (withAudio) {
                    mapOf(
                        "clip" to AudioClipPlacement("clip", "sound", 0, "Tone", 0, 12),
                    )
                } else {
                    emptyMap()
                },
            )
            val document =
                ProjectSaveDocument(
                    project = project,
                    frameLayers = (0 until 12).associateWith {
                        listOf(layer)
                    },
                    totalFrames = 12,
                )
            val repository = ProjectRepository(files)
            val manifest =
                ProjectManifest(
                    projectId = id,
                    displayName = "Test",
                    stage = stage,
                    fps = 12,
                    totalFrames = 12,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 1,
                )
            requireValue(repository.save(manifest, document))
            // Recreate every repository: export must use disk, not the editor's state or bitmap RAM.
            val freshFiles = DocumentFileStore(root)
            videoFile = withContext(Dispatchers.IO) {
                requireValue(
                    ProjectExporter(
                        context,
                        freshFiles,
                        ActorRepository(freshFiles),
                        PerformanceRepository(freshFiles),
                        ProjectRepository(freshFiles),
                    ).export(id),
                )
            }
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(videoFile.path)
                val videoTrack = (0 until extractor.trackCount).first {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")
                }
                val format = extractor.getTrackFormat(videoTrack)
                assertEquals(160, format.getInteger(MediaFormat.KEY_WIDTH))
                assertEquals(128, format.getInteger(MediaFormat.KEY_HEIGHT))
                assertEquals(if (withAudio) 2 else 1, extractor.trackCount)
                extractor.selectTrack(videoTrack)
                val times = mutableListOf<Long>()
                while (extractor.sampleTime >= 0) {
                    times.add(extractor.sampleTime)
                    extractor.advance()
                }
                assertEquals(12, times.size)
                times.forEachIndexed { frame, time ->
                    assertTrue(
                        "Frame $frame has incorrect timestamp: $time",
                        kotlin.math.abs(time - ExportTimeline.timeUs(frame.toLong(), 12)) <= 2,
                    )
                }
            } finally {
                extractor.release()
            }
        } finally {
            scope.cancel()
            videoFile?.delete()
            root.deleteRecursively()
        }
    }

    private fun <T> requireValue(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }
}
