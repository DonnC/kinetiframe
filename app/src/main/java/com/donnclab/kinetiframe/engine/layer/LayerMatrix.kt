package com.donnclab.kinetiframe.engine.layer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import com.donnclab.kinetiframe.models.Layer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Serializable project-side description of the spatial-temporal layer matrix.
 * Bitmaps are deliberately kept out of the model and owned by the cache.
 */
@Serializable
data class LayerMatrix(val layers: List<Layer> = emptyList()) {
    val orderedLayers: List<Layer>
        get() = layers.sortedBy { it.zIndex }

    fun layerAt(index: Int): Layer? = layers.getOrNull(index)

    fun activeLayer(index: Int): Layer? = layerAt(index)?.takeUnless { it.isLocked }
}

/**
 * Composites already-loaded layer bitmaps in document order.
 */
object LayerCompositor {
    suspend fun compose(
        layers: List<Layer>,
        frameIndex: Int,
        destination: Bitmap,
        sourceWidth: Int = destination.width,
        sourceHeight: Int = destination.height,
        bitmapFor: suspend (layer: Layer, frameIndex: Int) -> Bitmap?,
    ) = withContext(Dispatchers.Default) {
        if (destination.isRecycled) return@withContext

        val canvas = Canvas(destination)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.scale(destination.width.toFloat() / sourceWidth, destination.height.toFloat() / sourceHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        layers.sortedBy { it.zIndex }
            .filter { it.isVisible && it.opacity > 0f }
            .forEach { layer ->
                bitmapFor(layer, frameIndex)?.let { bitmap ->
                    if (!bitmap.isRecycled) {
                        paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).toInt()
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    }
                }
            }
    }
}
