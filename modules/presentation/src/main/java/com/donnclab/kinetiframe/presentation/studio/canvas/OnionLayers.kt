// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.layersForFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The drawing onion skin shows for [frame]: every visible layer combined, or only [activeLayerId]
 * when [activeLayerOnly]. Showing every layer is the default (R3-23): with the active layer alone,
 * a character on one layer lost its background's motion and nothing said which layer was showing.
 */
internal suspend fun onionFrameBitmap(
    cache: FrameCacheManager,
    state: StudioState,
    frame: Int,
    activeLayerId: String,
    activeLayerOnly: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Bitmap? {
    if (activeLayerOnly) return cache.getFrame(activeLayerId, frame)
    val drawn = state.layersForFrame(frame)
        .filter { it.isVisible }
        .sortedBy { it.zIndex }
        .mapNotNull { layer -> cache.getFrame(layer.id, frame)?.let { layer to it } }
    if (drawn.size <= 1) return drawn.firstOrNull()?.second
    return withContext(dispatcher) {
        val first = drawn.first().second
        val combined = Bitmap.createBitmap(first.width, first.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combined)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        drawn.forEach { (layer, bitmap) ->
            paint.alpha = (layer.opacity.coerceIn(0f, 1f) * OPAQUE).toInt()
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
        combined
    }
}

private const val OPAQUE = 255
