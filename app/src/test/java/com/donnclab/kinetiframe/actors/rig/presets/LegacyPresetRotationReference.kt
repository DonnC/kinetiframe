package com.donnclab.kinetiframe.actors.rig.presets

import kotlin.math.sin

internal object LegacyPresetRotationReference {
    fun offsets(applied: AppliedActorPreset, timeMicros: Long): Map<String, Float> {
        val recipe = applied.recipe
        val intensity = applied.intensity
        if (recipe.schemaVersion >= 2) {
            val duration = applied.durationMicros
            val time = timeMicros.coerceAtLeast(0)
            val progress = if (recipe.loop) {
                (time % duration).toDouble() / duration
            } else {
                time.coerceAtMost(duration).toDouble() /
                    duration
            }
            val values = recipe.channels.associate {
                it.boneId to
                    (LegacyPresetSamplingReference.curve(it.points, progress, recipe.loop) * intensity.coerceIn(0f, 2f))
            }
            if (recipe.channels.none { it.counterRotationBones.isNotEmpty() }) return values
            return values.toMutableMap().apply {
                recipe.channels.forEach { channel ->
                    if (channel.counterRotationBones.isNotEmpty()) {
                        this[channel.boneId] = values.getValue(channel.boneId) -
                            channel.counterRotationBones.sumOf { values.getValue(it).toDouble() }.toFloat()
                    }
                }
            }
        }
        val time = timeMicros.coerceAtLeast(0).toDouble() / 1_000_000.0
        val speedFactor = applied.speed.coerceIn(0.25f, 3f)
        val clampedIntensity = intensity.coerceIn(0f, 2f)
        return if (recipe.loop) {
            val phase = time * speedFactor / recipe.durationSeconds
            recipe.channels.associate { channel ->
                val radians =
                    phase * channel.cyclesPerLoop * 2 * Math.PI + Math.toRadians(channel.phaseDegrees.toDouble())
                channel.boneId to
                    (channel.offsetDegrees + channel.amplitudeDegrees * sin(radians).toFloat()) * clampedIntensity
            }
        } else {
            val progress = (time * speedFactor / recipe.durationSeconds).coerceIn(0.0, 1.0)
            val eased = progress * progress * (3 - 2 * progress)
            val settle = 1.0 - eased
            recipe.channels.associate { channel ->
                val radians =
                    progress * channel.cyclesPerLoop * 2 * Math.PI + Math.toRadians(channel.phaseDegrees.toDouble())
                val held = channel.offsetDegrees * eased.toFloat()
                val transient = channel.amplitudeDegrees * sin(radians).toFloat() * settle.toFloat()
                channel.boneId to (held + transient) * clampedIntensity
            }
        }
    }
}
