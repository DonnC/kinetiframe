package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootMotionPoint
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling

/** Root displacement is linear actor-space distance, never wrapped like joint angles. */
object RecordedRootMotionCurves {
    fun merge(
        original: List<RootMotionCurve>,
        samples: Map<String, List<RootMotionPoint>>,
        start: Long,
        end: Long,
        duration: Long,
        smoothness: Float,
    ): List<RootMotionCurve> {
        require(smoothness.isFinite() && smoothness in 0f..1f)
        require(start in 0..end && end <= duration)
        val previous = original.associateBy { it.rootId }
        val replacements = samples.map { (id, raw) ->
            RootMotionCurve(id, raw).validate(duration)
            val old = previous[id]
            val first = raw.first()
            val window = minOf(100_000L, (end - start).coerceAtLeast(1)).toFloat()
            val entering = raw.map { point ->
                val t = ((point.timeMicros - start) / window).coerceIn(0f, 1f)
                point.copy(x = first.x + (point.x - first.x) * t, y = first.y + (point.y - first.y) * t)
            }
            val filtered = entering.mapIndexed { i, point ->
                if (i == 0 || i == entering.lastIndex) {
                    point
                } else {
                    point.copy(
                        x = point.x + ((entering[i - 1].x + entering[i + 1].x) / 2 - point.x) * smoothness,
                        y = point.y + ((entering[i - 1].y + entering[i + 1].y) / 2 - point.y) * smoothness,
                    )
                }
            }
            val untouched = old?.points.orEmpty().filter { it.timeMicros < start || it.timeMicros > end }
            val next = untouched.firstOrNull { it.timeMicros > end }
            val fitted = if (smoothness == 0f) {
                filtered
            } else {
                TimedSampleReduction.indices(filtered.map { it.timeMicros }, .25f) { left, right, i, t ->
                    val a = filtered[left]
                    val b = filtered[right]
                    val point = filtered[i]
                    kotlin.math.hypot(point.x - (a.x + (b.x - a.x) * t), point.y - (a.y + (b.y - a.y) * t))
                }.map(filtered::get)
            }
            // Rejoin the existing path after a short bridge rather than jumping to its next key.
            val bridge = if (old != null && next != null) {
                val time = minOf(end + 100_000L, next.timeMicros)
                val value = RootMotionEvaluator.sample(
                    listOf(old),
                    time,
                    CurveSampling(durationMicros = duration),
                ).getValue(id)
                listOf(RootMotionPoint(time, value.x, value.y))
            } else {
                emptyList()
            }
            RootMotionCurve(
                id,
                (untouched + fitted + bridge).distinctBy { it.timeMicros }
                    .sortedBy { it.timeMicros },
            ).validate(duration)
        }
        return original.filterNot { it.rootId in samples } + replacements
    }
}
