// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.presets.PresetRootSampling
import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.BoundedCompilationCache

/** Immutable channels may be shared; compiled curves and their bounded lifetime belong to this instance. */
class PresetRootMotionSampler(capacity: Int = DEFAULT_CAPACITY) {
    private data class Entry(
        val channel: RootMotionChannel,
        val loop: Boolean,
        val x: CompiledMotionCurve,
        val y: CompiledMotionCurve,
    )

    private val cache = BoundedCompilationCache<Entry>(capacity)

    fun evaluate(
        channels: List<RootMotionChannel>,
        time: Long,
        rest: SceneGraph,
        sampling: PresetRootSampling,
    ): Map<String, RootTranslation> {
        if (channels.isEmpty()) return emptyMap()
        val duration = sampling.durationMicros
        val clock = if (sampling.loop) Math.floorMod(time, duration) else time.coerceIn(0, duration)
        val phase = (clock.toDouble() / duration * PHASE_UNITS).toLong()
        return channels.associate { channel ->
            val compiled = cache.getOrPut({
                it.loop == sampling.loop &&
                    (it.channel === channel || it.channel == channel)
            }) {
                val times = channel.points.map { (it.progress.toDouble() * PHASE_UNITS).toLong() }
                Entry(
                    channel,
                    sampling.loop,
                    CompiledMotionCurve(times, channel.points.map { it.x }, loop = sampling.loop),
                    CompiledMotionCurve(times, channel.points.map { it.y }, loop = sampling.loop),
                )
            }
            val scale = (rest.bones[channel.boneId]?.takeIf { it.parentId == null }?.length ?: 0f) *
                sampling.intensity.coerceIn(0f, 2f)
            channel.boneId to RootTranslation(compiled.x.sample(phase) * scale, compiled.y.sample(phase) * scale)
        }
    }

    fun clear() = cache.clear()

    companion object {
        private const val DEFAULT_CAPACITY = 16
        private const val PHASE_UNITS = 1_000_000
    }
}
