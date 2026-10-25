package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * A wrapper around a Bitmap and Canvas for off-screen rendering of animation frames.
 */
class FrameBitmapBuffer(width: Int, height: Int) {
    /**
     * The internal bitmap buffer where paths are baked.
     */
    val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private val canvas: Canvas = Canvas(bitmap)

    init {
        clear()
    }

    /**
     * Renders a vector path onto the internal bitmap buffer.
     *
     * @param path The vector path to draw.
     * @param paint The paint settings to use for drawing.
     * @param isEraser Whether this stroke should erase existing content.
     */
    fun drawStroke(path: Path, paint: Paint, isEraser: Boolean = false) {
        if (isEraser) {
            val originalXfermode = paint.xfermode
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawPath(path, paint)
            paint.xfermode = originalXfermode
        } else {
            canvas.drawPath(path, paint)
        }
    }

    /**
     * Clears the buffer to transparent color.
     */
    fun clear() {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    /**
     * Frees the underlying bitmap memory.
     * Should be called when the buffer is no longer needed.
     */
    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
