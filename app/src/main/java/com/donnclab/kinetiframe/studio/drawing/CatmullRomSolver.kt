package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow

/**
 * Utility to convert raw touch points into smooth spline segments using Catmull-Rom interpolation.
 */
object CatmullRomSolver {

    /**
     * Calculates Cubic Bezier control points for the segment between p1 and p2,
     * using p0 and p3 to determine the curvature (tangents).
     *
     * Adaptive tension is applied to prevent loops on tight curves.
     */
    fun calculateBezierControlPoints(
        p0: TouchPoint,
        p1: TouchPoint,
        p2: TouchPoint,
        p3: TouchPoint,
    ): BezierControlPoints {
        val d1 = hypot(p1.x - p0.x, p1.y - p0.y).coerceAtLeast(0.01f)
        val d2 = hypot(p2.x - p1.x, p2.y - p1.y).coerceAtLeast(0.01f)
        val d3 = hypot(p3.x - p2.x, p3.y - p2.y).coerceAtLeast(0.01f)

        // Centripetal parameterization alpha = 0.5 is best for avoiding loops
        val alpha = 0.5f
        val t1 = d1.pow(alpha)
        val t2 = d2.pow(alpha)
        val t3 = d3.pow(alpha)

        // Calculate tangents based on centripetal Catmull-Rom
        val m1X = (p2.x - p1.x + t2 * ((p1.x - p0.x) / t1 - (p2.x - p0.x) / (t1 + t2)))
        val m1Y = (p2.y - p1.y + t2 * ((p1.y - p0.y) / t1 - (p2.y - p0.y) / (t1 + t2)))
        val m2X = (p2.x - p1.x + t2 * ((p3.x - p2.x) / t3 - (p3.x - p1.x) / (t2 + t3)))
        val m2Y = (p2.y - p1.y + t2 * ((p3.y - p2.y) / t3 - (p3.y - p1.y) / (t2 + t3)))

        return BezierControlPoints(
            cp1X = p1.x + m1X / 3f,
            cp1Y = p1.y + m1Y / 3f,
            cp2X = p2.x - m2X / 3f,
            cp2Y = p2.y - m2Y / 3f,
        )
    }

    data class BezierControlPoints(val cp1X: Float, val cp1Y: Float, val cp2X: Float, val cp2Y: Float)

    /**
     * Interpolates between p1 and p2 using p0 and p3 as control points.
     * Uses the adaptive Bezier conversion for stability.
     */
    fun interpolate(p0: TouchPoint, p1: TouchPoint, p2: TouchPoint, p3: TouchPoint, steps: Int = 10): List<TouchPoint> {
        val cp = calculateBezierControlPoints(p0, p1, p2, p3)
        val result = mutableListOf<TouchPoint>()

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val tInv = 1f - t

            // Bezier cubic formula: (1-t)^3*P1 + 3(1-t)^2*t*CP1 + 3(1-t)*t^2*CP2 + t^3*P2
            val b0 = tInv * tInv * tInv
            val b1 = 3f * tInv * tInv * t
            val b2 = 3f * tInv * t * t
            val b3 = t * t * t

            val x = b0 * p1.x + b1 * cp.cp1X + b2 * cp.cp2X + b3 * p2.x
            val y = b0 * p1.y + b1 * cp.cp1Y + b2 * cp.cp2Y + b3 * p2.y

            // Linear interpolation for metadata
            val pressure = p1.pressure + (p2.pressure - p1.pressure) * t
            val velocity = p1.velocity + (p2.velocity - p1.velocity) * t
            val tilt = p1.tilt + (p2.tilt - p1.tilt) * t
            val orientation = p1.orientation + (p2.orientation - p1.orientation) * t
            val timestamp = p1.timestamp + ((p2.timestamp - p1.timestamp) * t).toLong()

            result.add(TouchPoint(x, y, max(0f, pressure), tilt, orientation, velocity, timestamp))
        }

        return result
    }
}
