package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset

/** Expression tracks have their own clock, but must not cut a repeating gait mid-cycle. */
object PerformanceDuration {
    fun expressionLoop(requested: Long, preset: AppliedActorPreset?): Long {
        if (preset == null || !preset.recipe.loop) return requested
        val period = preset.durationMicros
        val maximum = com.donnclab.kinetiframe.data.actor.PerformanceLimits.MAX_DURATION_MICROS
        if (period !in 1..maximum) return requested // Normal document validation reports unsupported duration.
        val cycles = (requested.coerceIn(1, maximum) + period - 1) / period
        return cycles.coerceAtMost(maximum / period) * period
    }
}
