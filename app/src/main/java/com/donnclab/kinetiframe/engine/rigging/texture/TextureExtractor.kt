package com.donnclab.kinetiframe.engine.rigging.texture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.ceil
import kotlin.math.floor

/** Crops only the opaque pixels inside a user's closed lasso selection. */
object TextureExtractor {
    /**
     * Tightest bounding box containing any non-transparent pixel, or null if [bitmap] is fully
     * transparent (nothing sketched yet). Backs the "tap a bone, then sketch" auto-bind flow,
     * where the whole layer is the source -- there's no hand-drawn lasso to bound it.
     */
    fun findContentBounds(bitmap: Bitmap): android.graphics.Rect? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var top = -1
        outerTop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if ((pixels[y * width + x] ushr 24) != 0) {
                    top = y
                    break@outerTop
                }
            }
        }
        if (top == -1) return null
        var bottom = top
        outerBottom@ for (y in height - 1 downTo top) {
            for (x in 0 until width) {
                if ((pixels[y * width + x] ushr 24) != 0) {
                    bottom = y
                    break@outerBottom
                }
            }
        }
        var left = 0
        outerLeft@ for (x in 0 until width) {
            for (y in top..bottom) {
                if ((pixels[y * width + x] ushr 24) != 0) {
                    left = x
                    break@outerLeft
                }
            }
        }
        var right = left
        outerRight@ for (x in width - 1 downTo left) {
            for (y in top..bottom) {
                if ((pixels[y * width + x] ushr 24) != 0) {
                    right = x
                    break@outerRight
                }
            }
        }
        return android.graphics.Rect(left, top, right + 1, bottom + 1)
    }

    fun extractLassoCrop(sourceFrame: Bitmap, lassoPath: Path): Bitmap {
        require(!sourceFrame.isRecycled) { "Cannot extract from a recycled bitmap." }

        val sourcePath = android.graphics.Path(lassoPath.asAndroidPath()).apply { close() }
        val bounds = RectF()
        sourcePath.computeBounds(bounds, true)

        val left = floor(bounds.left).toInt().coerceIn(0, sourceFrame.width)
        val top = floor(bounds.top).toInt().coerceIn(0, sourceFrame.height)
        val right = ceil(bounds.right).toInt().coerceIn(left, sourceFrame.width)
        val bottom = ceil(bounds.bottom).toInt().coerceIn(top, sourceFrame.height)
        val width = right - left
        val height = bottom - top
        require(width > 0 && height > 0) { "Invalid lasso crop bounds." }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val mask = android.graphics.Path(sourcePath).apply {
            offset(-left.toFloat(), -top.toFloat())
        }
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(mask, maskPaint)

        val extractPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(sourceFrame, -left.toFloat(), -top.toFloat(), extractPaint)
        extractPaint.xfermode = null
        return result
    }
}
