package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class ConstructionShape { CAPSULE, TRIANGLE, HEAD_GUIDE, THREE_POINT_ARC }

data class ShapeBounds(val startX: Float, val startY: Float, val endX: Float, val endY: Float) {
    val left get() = minOf(startX, endX)
    val right get() = maxOf(startX, endX)
    val top get() = minOf(startY, endY)
    val bottom get() = maxOf(startY, endY)
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
}

/** Pure deterministic construction-shape sampler. One result list is one independent stroke. */
object ConstructionShapeGeometry {
    fun sample(
        shape: ConstructionShape,
        bounds: ShapeBounds,
        peak: TouchPoint? = null,
        steps: Int = DEFAULT_STEPS,
    ): List<List<TouchPoint>> = when (shape) {
        ConstructionShape.CAPSULE -> listOf(capsule(bounds, steps))

        ConstructionShape.TRIANGLE -> listOf(cornerPreservingTriangle(bounds))

        ConstructionShape.HEAD_GUIDE -> listOf(
            ellipse(bounds, steps),
            straightConstructionLine(bounds.centerX, bounds.top, bounds.centerX, bounds.bottom),
            straightConstructionLine(bounds.left, bounds.centerY, bounds.right, bounds.centerY),
        )

        ConstructionShape.THREE_POINT_ARC -> listOf(arc(bounds, requireNotNull(peak), steps))
    }

    private fun capsule(bounds: ShapeBounds, steps: Int): List<TouchPoint> {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width <= 0f || height <= 0f) return listOf(point(bounds.centerX, bounds.centerY))
        val horizontal = width >= height
        val radius = minOf(width, height) / 2f
        val firstCenterX = if (horizontal) bounds.left + radius else bounds.centerX
        val firstCenterY = if (horizontal) bounds.centerY else bounds.top + radius
        val secondCenterX = if (horizontal) bounds.right - radius else bounds.centerX
        val secondCenterY = if (horizontal) bounds.centerY else bounds.bottom - radius
        val result = ArrayList<TouchPoint>(steps + 1)
        if (horizontal) {
            // The straight sections are part of the outline; omitting them leaves
            // two disconnected caps and produces a visibly incomplete capsule.
            result.add(point(firstCenterX, bounds.top))
            result.add(point(secondCenterX, bounds.top))
            for (i in 0..steps) {
                val a =
                    -PI / 2 + PI * i / steps
                result.add(
                    point(
                        secondCenterX + cos(a.toFloat()) * radius,
                        secondCenterY + sin(a.toFloat()) * radius,
                    ),
                )
            }
            result.add(point(firstCenterX, bounds.bottom))
            for (i in 0..steps) {
                val a =
                    PI / 2 + PI * i / steps
                result.add(
                    point(
                        firstCenterX + cos(a.toFloat()) * radius,
                        firstCenterY + sin(a.toFloat()) * radius,
                    ),
                )
            }
        } else {
            result.add(point(bounds.left, firstCenterY))
            result.add(point(bounds.left, secondCenterY))
            for (i in 0..steps) {
                val a =
                    PI + PI * i / steps
                result.add(
                    point(
                        secondCenterX + cos(a.toFloat()) * radius,
                        secondCenterY + sin(a.toFloat()) * radius,
                    ),
                )
            }
            result.add(point(bounds.right, firstCenterY))
            for (i in 0..steps) {
                val a =
                    0 + PI * i / steps
                result.add(
                    point(
                        firstCenterX + cos(a.toFloat()) * radius,
                        firstCenterY + sin(a.toFloat()) * radius,
                    ),
                )
            }
        }
        return result
    }

    private fun ellipse(bounds: ShapeBounds, steps: Int) = (0..steps).map { index ->
        val angle = (2.0 * PI * index / steps).toFloat()
        point(
            bounds.centerX + cos(angle) * (bounds.right - bounds.left) / 2f,
            bounds.centerY + sin(angle) * (bounds.bottom - bounds.top) / 2f,
        )
    }

    private fun arc(bounds: ShapeBounds, peak: TouchPoint, steps: Int) = (0..max(2, steps)).map { index ->
        val t = index.toFloat() / max(2, steps)
        val inverse = 1f - t
        point(
            inverse * inverse * bounds.startX + 2f * inverse * t * peak.x + t * t * bounds.endX,
            inverse * inverse * bounds.startY + 2f * inverse * t * peak.y + t * t * bounds.endY,
        )
    }

    private fun point(x: Float, y: Float) = TouchPoint(x, y, pressure = 1f)
    private fun straightConstructionLine(x1: Float, y1: Float, x2: Float, y2: Float): List<TouchPoint> =
        listOf(point(x1, y1), point(x1, y1), point(x2, y2), point(x2, y2))
    private fun cornerPreservingTriangle(bounds: ShapeBounds): List<TouchPoint> {
        val a = point(bounds.centerX, bounds.top)
        val b = point(bounds.right, bounds.bottom)
        val c = point(bounds.left, bounds.bottom)
        return listOf(a, a, a, b, b, b, c, c, c, a, a, a)
    }
    private const val DEFAULT_STEPS = 48
}
