// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.geometry

/** Platform-neutral, immutable rectangle in document pixels. Compose conversion is owned by the screens. */
data class Rect2(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: Point2 get() = Point2((left + right) / 2f, (top + bottom) / 2f)

    fun translate(dx: Float, dy: Float) = Rect2(left + dx, top + dy, right + dx, bottom + dy)

    /** The smallest rectangle holding both. */
    fun union(other: Rect2) =
        Rect2(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

    companion object {
        val Zero = Rect2(0f, 0f, 0f, 0f)

        /** The bounds of [points], or null when there are none. */
        fun around(points: List<Point2>): Rect2? = points.takeIf { it.isNotEmpty() }?.let {
            Rect2(it.minOf(Point2::x), it.minOf(Point2::y), it.maxOf(Point2::x), it.maxOf(Point2::y))
        }
    }
}
