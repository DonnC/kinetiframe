// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve

/** Immutable source revisions may be shared; compilation-cache lifetime belongs to this sampler. */
class MotionCurveSampler(limits: CurveCacheLimits = CurveCacheLimits()) {
    private data class RotationEntry(val source: RotationCurve, val periodic: Boolean, val curve: CompiledMotionCurve)
    private data class RootEntry(
        val source: RootMotionCurve,
        val periodic: Boolean,
        val x: CompiledMotionCurve,
        val y: CompiledMotionCurve,
    )

    private val rotations = BoundedCompilationCache<RotationEntry>(limits.rotations)
    private val roots = BoundedCompilationCache<RootEntry>(limits.roots)

    fun rotations(
        curves: List<RotationCurve>,
        time: Long,
        sampling: CurveSampling = CurveSampling(),
    ): Map<String, Float> =
        curves.filterNot { sampling.excluded(time) { it.points.first().timeMicros } }.associate { source ->
            val periodic =
                sampling.loop && sampling.periodic(source.points.first().timeMicros, source.points.last().timeMicros)
            val compiled = rotations.getOrPut({
                it.periodic == periodic && (it.source === source || it.source == source)
            }) {
                RotationEntry(
                    source,
                    periodic,
                    CompiledMotionCurve(
                        source.points.map { it.timeMicros },
                        source.points.map { it.degrees },
                        angular = true,
                        loop = periodic,
                    ),
                )
            }
            source.boneId to compiled.curve.sample(time, MotionInterpolation.curve(source.interpolation))
        }

    fun rootTranslations(
        curves: List<RootMotionCurve>,
        time: Long,
        sampling: CurveSampling = CurveSampling(),
    ): Map<String, RootTranslation> =
        curves.filterNot { sampling.excluded(time) { it.points.first().timeMicros } }.associate { source ->
            // Unlike rotation channels, root seams require an explicit enclosing duration.
            val periodic = sampling.loop && sampling.durationMicros != null &&
                sampling.periodic(source.points.first().timeMicros, source.points.last().timeMicros)
            val compiled = roots.getOrPut({
                it.periodic == periodic && (it.source === source || it.source == source)
            }) {
                val times = source.points.map { it.timeMicros }
                RootEntry(
                    source,
                    periodic,
                    CompiledMotionCurve(times, source.points.map { it.x }, loop = periodic),
                    CompiledMotionCurve(times, source.points.map { it.y }, loop = periodic),
                )
            }
            val interpolation = MotionInterpolation.curve(source.interpolation)
            source.rootId to
                RootTranslation(compiled.x.sample(time, interpolation), compiled.y.sample(time, interpolation))
        }

    fun clear() {
        rotations.clear()
        roots.clear()
    }
}
