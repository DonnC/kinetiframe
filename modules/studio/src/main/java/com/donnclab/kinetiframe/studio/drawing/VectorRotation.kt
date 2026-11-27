package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.geometry.Point2

/** [value] rotated by [degrees] around the origin. */
fun rotateVector(value: Point2, degrees: Float): Point2 {
    val radians = Math.toRadians(degrees.toDouble())
    val cosine = kotlin.math.cos(radians).toFloat()
    val sine = kotlin.math.sin(radians).toFloat()
    return Point2(
        value.x * cosine - value.y * sine,
        value.x * sine + value.y * cosine,
    )
}
