// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.geometry

import kotlin.math.sqrt

/** Platform-neutral coordinates. Compose conversion is owned by each presentation adapter. */
data class Point2(val x: Float, val y: Float) {
    operator fun plus(other: Point2) = Point2(x + other.x, y + other.y)
    operator fun minus(other: Point2) = Point2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Point2(x * scale, y * scale)
    operator fun div(scale: Float) = Point2(x / scale, y / scale)
    fun getDistance(): Float = sqrt(x * x + y * y)
}
