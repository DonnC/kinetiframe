package com.donnclab.kinetiframe.studio.drawing

import androidx.compose.ui.geometry.Offset

/** [value] rotated by [degrees] around the origin. */
fun rotateVector(value: Offset, degrees: Float): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    val cosine = kotlin.math.cos(radians).toFloat()
    val sine = kotlin.math.sin(radians).toFloat()
    return Offset(
        value.x * cosine - value.y * sine,
        value.x * sine + value.y * cosine,
    )
}
