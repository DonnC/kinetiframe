package com.donnclab.kinetiframe.engine.shape

import com.donnclab.kinetiframe.engine.model.TouchPoint
import kotlin.math.hypot

/**
 * Stateful, UI-free construction-shape gesture. Hosts render [preview] transiently
 * and commit [complete] exactly once; it never owns a bitmap or history entry.
 */
class ConstructionShapeSession(private val shape: ConstructionShape) {
    private var start: TouchPoint? = null
    private var peak: TouchPoint? = null
    private var current: TouchPoint? = null

    fun begin(point: TouchPoint) {
        start = point
        current = point
        peak = null
    }
    fun update(point: TouchPoint) {
        requireNotNull(start) { "Gesture has not started" }
        current = point
    }

    /** Optionally fixes a custom control peak; one-drag input uses an inferred peak. */
    fun confirmPeak(point: TouchPoint): Boolean {
        if (shape != ConstructionShape.THREE_POINT_ARC || start == null) return false
        peak = point
        current = point
        return true
    }

    fun preview(): List<List<TouchPoint>>? = samples(requireComplete = false)

    fun complete(): List<List<TouchPoint>>? = samples(requireComplete = true)?.also { reset() }

    fun reset() {
        start = null
        peak = null
        current = null
    }

    private fun samples(requireComplete: Boolean): List<List<TouchPoint>>? {
        val origin = start ?: return null
        val end = current ?: return null
        if (requireComplete && origin.x == end.x && origin.y == end.y) return null
        val arcPeak = if (shape == ConstructionShape.THREE_POINT_ARC) {
            peak ?: inferredPeak(origin, end)
        } else {
            peak
        }
        return ConstructionShapeGeometry.sample(
            shape = shape,
            bounds = ShapeBounds(origin.x, origin.y, end.x, end.y),
            peak = arcPeak,
        )
    }

    /**
     * One-drag arc fallback: place a control point half a chord-length away from
     * the chord midpoint. The perpendicular is deterministic, so preview and
     * commit always agree without requiring a second gesture phase.
     */
    private fun inferredPeak(origin: TouchPoint, end: TouchPoint): TouchPoint {
        val dx = end.x - origin.x
        val dy = end.y - origin.y
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (length == 0f) return origin
        val midpointX = (origin.x + end.x) / 2f
        val midpointY = (origin.y + end.y) / 2f
        val normalX = -dy / length
        val normalY = dx / length
        val offset = length * 0.5f
        return TouchPoint(midpointX + normalX * offset, midpointY + normalY * offset)
    }
}
