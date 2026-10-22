package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.RootMotionCurve
import com.donnclab.kinetiframe.actors.data.RotationCurve
import com.donnclab.kinetiframe.actors.rig.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.BeforeTake
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling

internal object LegacyCurveSamplingReference {
    private data class CachedCurve(val source: RotationCurve, val loop: Boolean, val curve: CompiledMotionCurve)
    private val curvesCache = ArrayDeque<CachedCurve>()
    fun curveRotations(
        curves: List<RotationCurve>,
        time: Long,
        sampling: CurveSampling = CurveSampling(),
    ): Map<String, Float> = curves.filterNot {
        sampling.beforeTake == BeforeTake.PRESERVE_PRESET && time < it.points.first().timeMicros
    }.associate { source ->
        val periodic =
            sampling.loop && source.points.first().timeMicros == 0L &&
                (sampling.durationMicros == null || source.points.last().timeMicros == sampling.durationMicros)
        val compiled = synchronized(curvesCache) {
            curvesCache.firstOrNull { it.loop == periodic && (it.source === source || it.source == source) }?.curve
                ?: CompiledMotionCurve(
                    source.points.map {
                        it.timeMicros
                    },
                    source.points.map { it.degrees },
                    angular = true,
                    loop = periodic,
                ).also {
                    curvesCache.addLast(CachedCurve(source, periodic, it))
                    if (curvesCache.size > 128) curvesCache.removeFirst()
                }
        }
        val interpolation = source.interpolation.engineInterpolation()
        source.boneId to compiled.sample(time, interpolation)
    }
    private data class Cached(
        val source: RootMotionCurve,
        val periodic: Boolean,
        val x: CompiledMotionCurve,
        val y: CompiledMotionCurve,
    )
    private val cache = ArrayDeque<Cached>()
    fun sample(curves: List<RootMotionCurve>, time: Long, sampling: CurveSampling): Map<String, RootTranslation> =
        curves.filterNot {
            sampling.beforeTake == BeforeTake.PRESERVE_PRESET && time < it.points.first().timeMicros
        }.associate { source ->
            val periodic =
                sampling.loop && source.points.first().timeMicros == 0L &&
                    source.points.last().timeMicros == sampling.durationMicros
            val compiled = synchronized(cache) {
                cache.firstOrNull { it.periodic == periodic && (it.source === source || it.source == source) } ?: run {
                    val times = source.points.map { it.timeMicros }
                    Cached(
                        source,
                        periodic,
                        CompiledMotionCurve(times, source.points.map { it.x }, loop = periodic),
                        CompiledMotionCurve(times, source.points.map { it.y }, loop = periodic),
                    ).also {
                        cache.addLast(it)
                        if (cache.size > 32) cache.removeFirst()
                    }
                }
            }
            val interpolation = source.interpolation.engineInterpolation()
            source.rootId to
                RootTranslation(compiled.x.sample(time, interpolation), compiled.y.sample(time, interpolation))
        }
}
