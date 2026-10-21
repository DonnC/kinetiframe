package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.engine.model.TouchPoint
import kotlin.math.hypot
import kotlin.math.max

/**
 * Filter for raw input points to remove noise and simplify paths.
 */
object InputFilter {

    private const val NOISE_THRESHOLD = 0.5f
    private const val LOW_VELOCITY_THRESHOLD = 100f // px per second

    /**
     * Filters out points that are considered noise.
     * Adaptive Threshold: Ignore noise < 0.5px when velocity is low.
     */
    fun adaptiveThreshold(current: TouchPoint, last: TouchPoint?, considerDynamics: Boolean = true): Boolean {
        if (last == null) return true
        if (current.timestamp <= last.timestamp) return false
        // A stationary pen changing force/angle is an intentional width/nib change.
        if (considerDynamics && (
                kotlin.math.abs(current.pressure - last.pressure) >= 0.01f ||
                    kotlin.math.abs(current.tilt - last.tilt) >= 0.01f ||
                    kotlin.math.abs(current.orientation - last.orientation) >= 0.01f
                )
        ) {
            return true
        }

        val distance = hypot(current.x - last.x, current.y - last.y)
        val deltaTime = max(1L, current.timestamp - last.timestamp) / 1000f
        val velocity = distance / deltaTime

        return if (velocity < LOW_VELOCITY_THRESHOLD) {
            distance >= NOISE_THRESHOLD
        } else {
            true
        }
    }

    /**
     * Simplifies a path using the Douglas-Peucker algorithm.
     * @param points The original list of points.
     * @param epsilon The maximum perpendicular distance for a point to be included.
     */
    fun simplifyDouglasPeucker(points: List<TouchPoint>, epsilon: Float): List<TouchPoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var index = 0

        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                index = i
                maxDist = dist
            }
        }

        return if (maxDist > epsilon) {
            val left = simplifyDouglasPeucker(points.subList(0, index + 1), epsilon)
            val right = simplifyDouglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(p: TouchPoint, start: TouchPoint, end: TouchPoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y

        if (dx == 0f && dy == 0f) {
            return hypot(p.x - start.x, p.y - start.y)
        }

        val numerator = Math.abs(dy * p.x - dx * p.y + end.x * start.y - end.y * start.x)
        val denominator = hypot(dx, dy)

        return numerator / denominator
    }
}
