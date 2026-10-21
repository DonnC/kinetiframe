// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.PerformanceTiming
import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming

object PerformanceClock {
    fun normalize(timing: PerformanceTiming, timeMicros: Long, standalonePreset: PresetTiming? = null): Long =
        when (standalonePreset?.mode) {
            null -> when (timing.playback) {
                PerformancePlayback.LOOP -> Math.floorMod(timeMicros, timing.durationMicros)

                PerformancePlayback.ONE_SHOT, PerformancePlayback.END_HOLD -> timeMicros.coerceIn(
                    0,
                    timing.durationMicros,
                )
            }

            PresetClockMode.ONE_SHOT -> timeMicros.coerceIn(0, standalonePreset.durationMicros)

            // Legacy preset-only loops clamp negative input instead of wrapping it like document loops.
            PresetClockMode.REPEAT -> if (timing.playback == PerformancePlayback.LOOP) {
                timeMicros.coerceAtLeast(0) % standalonePreset.durationMicros
            } else {
                timeMicros.coerceIn(0, timing.durationMicros)
            }
        }
}
