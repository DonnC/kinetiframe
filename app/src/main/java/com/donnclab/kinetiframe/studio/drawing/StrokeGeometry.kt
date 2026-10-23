package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Shared, deterministic geometry policy for every pressure stroke.
 *
 * This deliberately contains no UI or bitmap state.  Preview and baking can therefore
 * use the same Catmull-Rom samples instead of drifting apart as the implementation evolves.
 */
object StrokeGeometry {
    private const val MAX_RENDER_STEP_LENGTH = 4f
    private const val MAX_STEPS_PER_SOURCE_SEGMENT = 32

    /**
     * Calculates the width for a specific point on the stroke, incorporating both
     * stylus pressure and movement velocity for professional tapering.
     */
    fun calculateDynamicWidth(baseWidth: Float, pressure: Float, velocity: Float): Float {
        val pFactor = (0.45f + pressure.coerceIn(0f, 1f) * 0.75f)
        // Velocity-based tapering: higher speed = thinner line.
        // Fast "flicks" (v > 3000px/s) will taper down by up to 30%.
        val vFactor = (1f - (velocity / 3000f).coerceIn(0f, 1f) * 0.3f)
        return baseWidth * pFactor * vFactor
    }

    /**
     * Calculates a unit normal vector (perpendicular) to the segment from p1 to p2.
     */
    fun calculateNormal(p1: TouchPoint, p2: TouchPoint): Pair<Float, Float> {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        return Pair(-dy / len, dx / len)
    }

    fun interpolationSteps(
        start: TouchPoint,
        end: TouchPoint,
        maxRenderStepLength: Float = MAX_RENDER_STEP_LENGTH,
    ): Int {
        require(maxRenderStepLength > 0f) { "maxRenderStepLength must be positive" }
        val distance = hypot(end.x - start.x, end.y - start.y)
        return ceil(distance / maxRenderStepLength).toInt()
            .coerceIn(1, MAX_STEPS_PER_SOURCE_SEGMENT)
    }

    fun interpolatedSegment(p0: TouchPoint, p1: TouchPoint, p2: TouchPoint, p3: TouchPoint): List<TouchPoint> =
        buildList {
            add(p1)
            forEachInterpolatedEnd(p0, p1, p2, p3) { add(it) }
        }

    fun forEachInterpolatedEnd(
        p0: TouchPoint,
        p1: TouchPoint,
        p2: TouchPoint,
        p3: TouchPoint,
        block: (TouchPoint) -> Unit,
    ) {
        val cp = CatmullRomSolver.calculateBezierControlPoints(p0, p1, p2, p3)
        CubicStrokeSampler.forEachEnd(p1.x, p1.y, cp.cp1X, cp.cp1Y, cp.cp2X, cp.cp2Y, p2.x, p2.y) { x, y, t ->
            block(
                TouchPoint(
                    x,
                    y,
                    pressure = p1.pressure + (p2.pressure - p1.pressure) * t,
                    tilt = p1.tilt + (p2.tilt - p1.tilt) * t,
                    orientation = p1.orientation + (p2.orientation - p1.orientation) * t,
                    velocity = p1.velocity + (p2.velocity - p1.velocity) * t,
                    timestamp = p1.timestamp + ((p2.timestamp - p1.timestamp) * t).toLong(),
                ),
            )
        }
    }

    fun forEachSegment(
        points: List<TouchPoint>,
        startSourceSegment: Int = 0,
        block: (Int, TouchPoint, TouchPoint) -> Unit,
    ) {
        if (points.size < 2) return
        for (i in startSourceSegment.coerceIn(0, points.lastIndex) until points.lastIndex) {
            val p0 = points[max(0, i - 1)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[min(points.lastIndex, i + 2)]
            var previous = p1
            forEachInterpolatedEnd(p0, p1, p2, p3) { current ->
                block(i, previous, current)
                previous = current
            }
        }
    }
}
