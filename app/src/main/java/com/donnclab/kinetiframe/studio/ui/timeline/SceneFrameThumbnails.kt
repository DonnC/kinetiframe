package com.donnclab.kinetiframe.studio.ui.timeline

import android.graphics.Bitmap
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.media.image.FrameImageDecoder
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.layersForFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.render.ProjectFrameCompositor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** One reusable compositor per visible batch, with only small independent outputs retained. */
suspend fun sceneFrameThumbnails(
    state: StudioState,
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
