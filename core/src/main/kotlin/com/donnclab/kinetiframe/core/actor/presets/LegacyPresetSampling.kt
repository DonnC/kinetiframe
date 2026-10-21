// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode

data class LegacyPresetSampling(
    val durationSeconds: Float,
    val speed: Float = 1f,
    val intensity: Float = 1f,
    val mode: PresetClockMode = PresetClockMode.REPEAT,
)
