// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming
import com.donnclab.kinetiframe.core.actor.presets.LegacyPresetSampling
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import kotlin.math.sin

/** Copies channel lists so later caller edits cannot change an evaluated recipe snapshot. */
class PresetRotationProgram(channels: List<PresetRotationChannel>) {
    private val channels = channels.map {
        it.copy(points = it.points.toList(), counterRotationBones = it.counterRotationBones.toList())
    }

    fun authored(timeMicros: Long, timing: PresetTiming, intensity: Float = 1f): Map<String, Float> {
        val duration = timing.durationMicros
        val time = timeMicros.coerceAtLeast(0)
        val loop = timing.mode == PresetClockMode.REPEAT
        val progress = if (loop) {
            (time % duration).toDouble() / duration
        } else {
            time.coerceAtMost(duration).toDouble() /
                duration
        }
        val values = channels.associate {
            it.boneId to
                (PresetCurve.evaluate(it.points, progress, loop) * intensity.coerceIn(0f, 2f))
        }
        if (channels.none { it.counterRotationBones.isNotEmpty() }) return values
        return values.toMutableMap().apply {
            channels.forEach { channel ->
                if (channel.counterRotationBones.isNotEmpty()) {
                    // Compensate evaluated angles: nonlinear tangents do not commute with negated knot sums.
                    this[channel.boneId] = values.getValue(channel.boneId) -
                        channel.counterRotationBones.sumOf { values.getValue(it).toDouble() }.toFloat()
                }
            }
        }
    }

    fun legacy(timeMicros: Long, sampling: LegacyPresetSampling): Map<String, Float> {
        val time = timeMicros.coerceAtLeast(0).toDouble() / MICROS_PER_SECOND
        val speedFactor = sampling.speed.coerceIn(MIN_SPEED, MAX_SPEED)
        val intensity = sampling.intensity.coerceIn(0f, 2f)
        return when (sampling.mode) {
            PresetClockMode.REPEAT -> {
                // Keep the original unbounded phase; wrapping it changes long-time floating-point results.
                val phase = time * speedFactor / sampling.durationSeconds
                channels.associate { channel ->
                    val oscillator = channel.oscillation
                    val radians =
                        phase * oscillator.cyclesPerLoop * 2 * Math.PI +
                            Math.toRadians(oscillator.phaseDegrees.toDouble())
                    channel.boneId to
                        (oscillator.offsetDegrees + oscillator.amplitudeDegrees * sin(radians).toFloat()) * intensity
                }
            }

            PresetClockMode.ONE_SHOT -> {
                val progress = (time * speedFactor / sampling.durationSeconds).coerceIn(0.0, 1.0)
                val eased = progress * progress * (3 - 2 * progress)
                val settle = 1.0 - eased
                channels.associate { channel ->
                    val oscillator = channel.oscillation
                    val radians =
                        progress * oscillator.cyclesPerLoop * 2 * Math.PI +
                            Math.toRadians(oscillator.phaseDegrees.toDouble())
                    val held = oscillator.offsetDegrees * eased.toFloat()
                    val transient = oscillator.amplitudeDegrees * sin(radians).toFloat() * settle.toFloat()
                    channel.boneId to (held + transient) * intensity
                }
            }
        }
    }

    companion object {
        private const val MICROS_PER_SECOND = 1_000_000.0
        private const val MIN_SPEED = .25f
        private const val MAX_SPEED = 3f
    }
}
