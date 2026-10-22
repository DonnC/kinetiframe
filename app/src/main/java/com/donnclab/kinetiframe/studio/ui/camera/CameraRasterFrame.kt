package com.donnclab.kinetiframe.studio.ui.camera

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.cache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.graphics.createBitmap
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.displayFrame
import com.donnclab.kinetiframe.studio.drawing.LayerCompositor
import com.donnclab.kinetiframe.studio.layersForFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.render.completedPreviewFrames

/** Camera-dialog playback has its own frame clock. Never reuse the editor's stationary raster.
 * Two small buffers bound memory; frame preparation is conflated and finishes before disposal. */
@Composable
internal fun cameraRasterFrame(state: StudioState, cache: FrameCacheManager?, enabled: Boolean): Bitmap? {
    if (!enabled || cache == null) return null
    val stage = state.project.stage
    var prepared by remember(cache, stage) { mutableStateOf<Bitmap?>(null) }
    val latest by rememberUpdatedState(
        state.animation.displayFrame() to state.layersForFrame(state.animation.displayFrame()),
    )
    LaunchedEffect(cache, stage) {
        val scale = minOf(1f, 960f / maxOf(stage.width, stage.height))
        val buffers =
            List(2) {
                createBitmap(
                    (stage.width * scale).toInt().coerceAtLeast(1),
                    (stage.height * scale).toInt().coerceAtLeast(1),
                )
            }
        var index = 0
        try {
            snapshotFlow { latest }.completedPreviewFrames { (frame, layers) ->
                val target = buffers[index]
                LayerCompositor.compose(layers, frame, target, stage.width, stage.height) { layer, requested ->
                    cache.getFrame(layer.id, requested)
                }
                index = 1 - index
                target
            }.collect { prepared = it.second }
        } finally {
            buffers.forEach { it.recycle() }
        }
    }
    return prepared
}
