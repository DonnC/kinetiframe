package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurvePoint
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import kotlin.math.abs

/** Bounded, part-scoped take fitting. Untouched curves retain their exact original objects.
 * Smooth angular samples, not artwork switches. Preserve deliberate reversals and endpoints;
 * ordinary smooth interpolation then fills gaps without easing every pointer event to a stop.
 */
object RecordedMotionCurves {
    fun fit(points: List<RotationCurvePoint>, smoothness: Float): List<RotationCurvePoint> {
        require(smoothness.isFinite() && smoothness in 0f..1f)
        require(points.size <= RotationCurve.MAX_POINTS)
        require(points.zipWithNext().all { (a, b) -> a.timeMicros < b.timeMicros })
        require(points.all { it.degrees.isFinite() })
        if (points.size < 3) return points
        val unwrapped = FloatArray(points.size)
        unwrapped[0] = points[0].degrees
        for (i in 1..points.lastIndex) {
            unwrapped[i] = unwrapped[i - 1] +
                CompiledMotionCurve.shortestAngle(points[i].degrees - points[i - 1].degrees)
        }
        val filteredPoints = points.mapIndexed { i, point ->
            val value = unwrapped[i]
            val filtered = if (i == 0 || i == points.lastIndex) {
                value
            } else {
                val before = unwrapped[i - 1]
                val after = unwrapped[i + 1]
                // Keep large intentional accents, while suppressing small finger tremors.
                val accent = (value - before) * (after - value) < 0 &&
                    minOf(abs(value - before), abs(after - value)) > 3f
                if (accent) value else value + ((before + after) / 2f - value) * smoothness
            }
            point.copy(degrees = CompiledMotionCurve.shortestAngle(filtered))
        }
        if (smoothness == 0f) return points
        // Error-bounded iterative reduction preserves timing and removes stationary/linear
        // samples. No recursive stack and no unbounded pointer-event list on the UI thread.
        val values = FloatArray(filteredPoints.size)
        values[0] = filteredPoints[0].degrees
        for (i in 1..filteredPoints.lastIndex) {
            values[i] = values[i - 1] +
                CompiledMotionCurve.shortestAngle(filteredPoints[i].degrees - filteredPoints[i - 1].degrees)
        }
        return TimedSampleReduction.indices(points.map { it.timeMicros }, .25f) { left, right, i, fraction ->
            abs(values[i] - (values[left] + (values[right] - values[left]) * fraction))
        }.map(filteredPoints::get)
    }

    fun merge(
        original: List<RotationCurve>,
        samples: Map<String, List<RotationCurvePoint>>,
        start: Long,
        end: Long,
        duration: Long,
        smoothness: Float,
    ): List<RotationCurve> {
        require(start in 0..end && end <= duration)
        val previous = original.associateBy { it.boneId }
        val replacements = samples.map { (id, raw) ->
            val old = previous[id]
            val first = raw.firstOrNull()?.degrees ?: error("A recorded part needs samples")
            val blendWindow = minOf(100_000L, (end - start).coerceAtLeast(1L)).toFloat()
            val blended = raw.map { point ->
                val fraction = ((point.timeMicros - start) / blendWindow).coerceIn(0f, 1f)
                point.copy(
                    degrees = CompiledMotionCurve.shortestAngle(
                        first +
                            CompiledMotionCurve.shortestAngle(point.degrees - first) * fraction,
                    ),
                )
            }
            // Fit after blending so reduction cannot erase the point where entrance ends.
            val entering = fit(blended, smoothness)
            val untouched = old?.points.orEmpty().filter { it.timeMicros < start || it.timeMicros > end }
            // Resume an existing later movement through a short bridge, rather than snapping
            // directly from the recorded pose to its next authored key.
            val next = untouched.firstOrNull { it.timeMicros > end }
            val bridge = if (old != null && next != null) {
                val time = minOf(end + 100_000, next.timeMicros)
                val curve =
                    CompiledMotionCurve(old.points.map { it.timeMicros }, old.points.map { it.degrees }, angular = true)
                listOf(RotationCurvePoint(time, curve.sample(time, old.interpolation.engineInterpolation())))
            } else {
                emptyList()
            }
            val points = (untouched + entering + bridge).distinctBy { it.timeMicros }
            // Never silently drop motion to fit the durable document budget.
            RotationCurve(id, CurveInterpolation.SMOOTH, points.sortedBy { it.timeMicros }).validate(duration)
        }
        return original.filterNot { it.boneId in samples } + replacements
    }
}
