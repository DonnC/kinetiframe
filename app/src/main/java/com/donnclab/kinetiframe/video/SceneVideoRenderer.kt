package com.donnclab.kinetiframe.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.studio.drawing.Affine2D

/** Allocated outside Compose draw phases; independent of raster erasing. */
class SceneVideoRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val target = RectF()
    private val source = android.graphics.Rect()
    fun draw(canvas: Canvas, bitmap: Bitmap, clip: VideoClipPlacement, preview: Affine2D = Affine2D()) {
        if (bitmap.isRecycled) return
        matrix.setValues((preview * clip.selectionTransform).androidValues())
        val framing = com.donnclab.kinetiframe.video.VideoFraming.resolve(clip, bitmap.width, bitmap.height)
        with(framing.destination) { target.set(left, top, right, bottom) }
        with(framing.source) {
            source.set(
                left.toInt(),
                top.toInt(),
                right.toInt().coerceAtLeast(left.toInt() + 1),
                bottom.toInt().coerceAtLeast(top.toInt() + 1),
            )
        }
        canvas.save()
        canvas.concat(matrix)
        canvas.drawBitmap(bitmap, source, target, paint)
        canvas.restore()
    }
}
