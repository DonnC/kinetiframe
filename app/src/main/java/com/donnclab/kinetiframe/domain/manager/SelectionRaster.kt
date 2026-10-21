package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidPath

/** One shared raster-mask contract for selection preview and background removal. */
internal object SelectionRaster {
    data class Cut(val bitmap: Bitmap, val bounds: Rect)

    fun extract(source: Bitmap, path: androidx.compose.ui.graphics.Path?, rect: Rect?): Cut? {
        if (path == null && rect == null) return null
        val area = rect ?: path?.getBounds() ?: return null
        val left = kotlin.math.floor(area.left).toInt().coerceIn(0, source.width)
        val top = kotlin.math.floor(area.top).toInt().coerceIn(0, source.height)
        val right = kotlin.math.ceil(area.right).toInt().coerceIn(0, source.width)
        val bottom = kotlin.math.ceil(area.bottom).toInt().coerceIn(0, source.height)
        if (right <= left || bottom <= top) return null
        val masked = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(masked)
        canvas.save()
        canvas.translate(-left.toFloat(), -top.toFloat())
        if (path !=
            null
        ) {
            canvas.clipPath(path.asAndroidPath())
        } else {
            rect?.let { canvas.clipRect(it.left, it.top, it.right, it.bottom) }
        }
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restore()
        val bounds = alphaBounds(masked)
        if (bounds == null) {
            masked.recycle()
            return null
        }
        val cropped = Bitmap.createBitmap(
            masked,
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.width.toInt(),
            bounds.height.toInt(),
        )
        if (cropped !== masked) masked.recycle()
        return Cut(cropped, bounds.translate(androidx.compose.ui.geometry.Offset(left.toFloat(), top.toFloat())))
    }

    fun clear(source: Bitmap, path: androidx.compose.ui.graphics.Path?, rect: Rect?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        Canvas(source).also { canvas ->
            if (path !=
                null
            ) {
                canvas.drawPath(path.asAndroidPath(), paint)
            } else {
                rect?.let { canvas.drawRect(it.left, it.top, it.right, it.bottom, paint) }
            }
        }
    }

    private fun alphaBounds(bitmap: Bitmap): Rect? {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            row.forEachIndexed { x, color ->
                if ((color ushr 24) !=
                    0
                ) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        return if (right <
            left
        ) {
            null
        } else {
            Rect(left.toFloat(), top.toFloat(), (right + 1).toFloat(), (bottom + 1).toFloat())
        }
    }
}
