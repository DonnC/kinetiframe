package com.donnclab.kinetiframe.actors.rig.texture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/** Adds a small overlap around a cutout so rotated parts do not expose gaps. */
object JointProcessor {
    fun dilateJointEdges(source: Bitmap, dilationRadius: Int = 8): Bitmap {
        require(!source.isRecycled) { "Cannot dilate a recycled bitmap." }
        require(dilationRadius >= 0) { "Dilation radius must not be negative." }
        if (dilationRadius == 0) return source.copy(Bitmap.Config.ARGB_8888, true)

        val output = Bitmap.createBitmap(
            source.width + dilationRadius * 2,
            source.height + dilationRadius * 2,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val center = dilationRadius.toFloat()

        // Radial bitmap draws let Android perform the smear without a slow pixel loop.
        for (angle in 0 until 360 step 45) {
            val radians = Math.toRadians(angle.toDouble())
            canvas.drawBitmap(
                source,
                center + (cos(radians) * dilationRadius).toFloat(),
                center + (sin(radians) * dilationRadius).toFloat(),
                paint,
            )
        }
        canvas.drawBitmap(source, center, center, paint)
        return output
    }
}
