package com.donnclab.kinetiframe.media.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.donnclab.kinetiframe.core.document.ProjectDocument
import kotlin.math.roundToInt

/** One sequential lane, one native decoder. Decode off the main thread.
 * Returned bitmaps belong to the caller, not a shared mutable cache. */
class VideoFrameDecoder(private val maxEdge: Int = 1280) : AutoCloseable {
    private var path: String? = null
    private var retriever: MediaMetadataRetriever? = null
    private var closed = false

    @Synchronized fun decode(project: ProjectDocument, frame: Int): Bitmap? {
        if (closed) return null
        val active = VideoTrackSync.activeClip(project.videoClips.values, frame, project.fps)
            ?: return null.also { releaseSource() }
        val clip = project.videoClips[active.clipId] ?: return null
        val source = project.assets[clip.assetId]?.sourcePath ?: return null
        return try {
            if (path != source) {
                releaseSource()
                val opened = MediaMetadataRetriever()
                try {
                    opened.setDataSource(source)
                } catch (failure: Exception) {
                    opened.release()
                    throw failure
                }
                retriever = opened
                path = source
            }
            val decoder = requireNotNull(retriever)
            val width =
                decoder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: maxEdge
            val height =
                decoder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: maxEdge
            val scale = minOf(1.0, maxEdge.toDouble() / maxOf(width, height).coerceAtLeast(1))
            decoder.getScaledFrameAtTime(
                (active.sourceOffsetSeconds * 1_000_000).toLong(),
                MediaMetadataRetriever.OPTION_CLOSEST,
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1),
            )
        } catch (failure: Exception) {
            Log.w("VideoFrameDecoder", "Video frame $frame unavailable", failure)
            null
        }
    }

    @Synchronized override fun close() {
        closed = true
        releaseSource()
    }
    private fun releaseSource() {
        retriever?.let { runCatching { it.release() } }
        retriever = null
        path = null
    }
}
