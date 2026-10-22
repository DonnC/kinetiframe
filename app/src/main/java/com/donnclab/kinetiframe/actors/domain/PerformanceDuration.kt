package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset

/** Expression tracks have their own clock, but must not cut a repeating gait mid-cycle. */
object PerformanceDuration {
    fun expressionLoop(requested: Long, preset: AppliedActorPreset?): Long {
        if (preset == null || !preset.recipe.loop) return requested
        val period = preset.durationMicros
        val maximum = com.donnclab.kinetiframe.actors.data.PerformanceLimits.MAX_DURATION_MICROS
        if (period !in 1..maximum) return requested // Normal document validation reports unsupported duration.
        val cycles = (requested.coerceIn(1, maximum) + period - 1) / period
        return cycles.coerceAtMost(maximum / period) * period
    }
}
