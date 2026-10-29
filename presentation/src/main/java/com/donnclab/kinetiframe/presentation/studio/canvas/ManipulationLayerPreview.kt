package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.donnclab.kinetiframe.core.document.Layer

/** Replaces pixels inside an isolated active layer, preserving lower/upper layers and opacity. */
internal class ManipulationLayerPreview {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val replacement = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val bounds = RectF()

    fun draw(
        canvas: Canvas,
        layers: List<Pair<Layer, Bitmap>>,
        activeId: String,
        stroke: ActiveStroke,
        width: Int,
        height: Int,
        bakedStrokeSeeds: Set<Long> = emptySet(),
    ): Boolean {
        val current = stroke.manipulationPreview()
        if (layers.isEmpty() ||
            (
                current == null &&
                    stroke.settledStrokes.none { it.brush.seed !in bakedStrokeSeeds && it.manipulationPreview != null }
                )
        ) {
            return false
        }
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        for ((layer, bitmap) in layers) {
            if (!layer.isVisible || layer.opacity <= 0 || bitmap.isRecycled) continue
            paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            if (layer.id != activeId) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                continue
            }
            val save = canvas.saveLayer(bounds, paint)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            for (settled in stroke.settledStrokes) {
                if (settled.brush.seed !in
                    bakedStrokeSeeds
                ) {
                    settled.manipulationPreview?.let {
                        if (!it.bitmap.isRecycled) {
                            canvas.drawBitmap(
                                it.bitmap,
                                it.left.toFloat(),
                                it.top.toFloat(),
                                replacement,
                            )
                        }
                    }
                }
            }
            current?.let {
                if (!it.bitmap.isRecycled) {
                    canvas.drawBitmap(
                        it.bitmap,
                        it.left.toFloat(),
                        it.top.toFloat(),
                        replacement,
                    )
                }
            }
            canvas.restoreToCount(save)
        }
        return true
    }
}
