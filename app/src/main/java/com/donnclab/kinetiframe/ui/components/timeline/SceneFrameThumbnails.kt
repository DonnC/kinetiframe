package com.donnclab.kinetiframe.ui.components.timeline

import android.graphics.Bitmap
import com.donnclab.kinetiframe.data.image.FrameImageDecoder
import com.donnclab.kinetiframe.domain.render.ActorFrameRenderer
import com.donnclab.kinetiframe.domain.render.ProjectFrameCompositor
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.layersForFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** One reusable compositor per visible batch, with only small independent outputs retained. */
suspend fun sceneFrameThumbnails(
    state: TimelineState,
    cache: FrameCacheManager,
    frames: Collection<Int>,
    actorRendererFactory: (() -> ActorFrameRenderer)? = null,
): Map<Int, Bitmap> = withContext(Dispatchers.IO) {
    if (frames.isEmpty()) return@withContext emptyMap()
    val background = state.project.backgroundAssetId?.let { state.project.assets[it]?.sourcePath }
        ?.let { path ->
            try {
                FrameImageDecoder.decode(512) { File(path).inputStream() }
            } catch (
                missing: IOException,
            ) {
                android.util.Log.w("FrameThumbnail", "Background unavailable", missing)
                null
            }
        }
    val compositor = ProjectFrameCompositor(
        state.project,
        frames.associateWith { state.layersForFrame(it) },
        cache,
        actorRendererFactory?.invoke(),
        background,
        outputMaxEdge = 256,
    )
    try {
        frames.mapNotNull { frame ->
            // The compositor reuses its output. Never hand that mutable output to a lazy cell.
            try {
                frame to compositor.composite(frame).copy(Bitmap.Config.ARGB_8888, false)
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (unavailable: Exception) {
                // A damaged resource must not crash the editor merely by scrolling its timeline.
                android.util.Log.w("FrameThumbnail", "Frame $frame unavailable", unavailable)
                null
            }
        }.toMap()
    } finally {
        compositor.release()
        background?.recycle()
    }
}
