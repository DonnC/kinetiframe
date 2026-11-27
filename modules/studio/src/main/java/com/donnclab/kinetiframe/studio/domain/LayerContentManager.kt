// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.LayerType
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What [LayerContentManager] needs from the session around a layer edit. */
data class LayerContentHooks(
    /** Bakes the drawing on the canvas and drops its undo history, before pixels move. */
    val settleDrawing: suspend () -> Unit,
    val loadFrame: suspend (Int) -> Unit,
)

/**
 * Layer edits that change pixels on every frame (R3-27): duplicate a layer and merge it into the one
 * below. The layer stack is shared by all frames, so each edit touches every frame's pixels once.
 */
class LayerContentManager(
    private val runtime: StudioRuntime,
    private val cache: FrameCacheManager,
    private val frameMutex: Mutex,
    private val hooks: LayerContentHooks,
) {
    fun duplicate(index: Int) = edit { total ->
        val stack = runtime.getState().currentFrameLayers()
        val source = stack.getOrNull(index)?.takeIf { it.type == LayerType.RASTER } ?: return@edit
        val copy = source.copy(id = "layer_copy_${System.nanoTime()}", name = "${source.name} copy")
        for (frame in 0 until total) {
            cache.getFrame(source.id, frame)?.let { pixels ->
                cache.putFrame(copy.id, frame, pixels.copy(Bitmap.Config.ARGB_8888, true))
            }
        }
        val updated = (stack.take(index + 1) + copy + stack.drop(index + 1)).mapIndexed { i, layer ->
            layer.copy(zIndex = i)
        }
        publish(updated, active = index + 1)
    }

    fun mergeDown(index: Int) = edit { total ->
        val stack = runtime.getState().currentFrameLayers()
        val top = stack.getOrNull(index) ?: return@edit
        val below = stack.getOrNull(index - 1) ?: return@edit
        if (top.type != LayerType.RASTER || below.type != LayerType.RASTER || below.isLocked) return@edit
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (top.opacity.coerceIn(0f, 1f) * OPAQUE).toInt() }
        for (frame in 0 until total) {
            val upper = cache.getFrame(top.id, frame) ?: continue
            val merged = cache.getFrame(below.id, frame)?.copy(Bitmap.Config.ARGB_8888, true)
                ?: Bitmap.createBitmap(upper.width, upper.height, Bitmap.Config.ARGB_8888)
            Canvas(merged).drawBitmap(upper, 0f, 0f, paint)
            cache.putFrame(below.id, frame, merged)
        }
        publish(stack.filterIndexed { i, _ -> i != index }.mapIndexed { i, layer -> layer.copy(zIndex = i) }, index - 1)
    }

    private fun edit(action: suspend (totalFrames: Int) -> Unit) {
        runtime.pending.launch(runtime.scope, runtime.dispatcher) {
            frameMutex.withLock {
                hooks.settleDrawing()
                action(runtime.getState().animation.totalFrames)
                hooks.loadFrame(runtime.getState().animation.currentFrame)
            }
        }
    }

    private fun publish(stack: List<Layer>, active: Int) {
        runtime.updateState { state ->
            state.copy(
                project = state.project.copy(layers = stack),
                layers = state.layers.copy(
                    layers = stack,
                    frameLayers = state.layers.frameLayers.keys.associateWith { stack },
                    activeLayerIndex = active.coerceIn(0, maxOf(0, stack.lastIndex)),
                ),
            )
        }
    }

    private companion object {
        const val OPAQUE = 255
    }
}
