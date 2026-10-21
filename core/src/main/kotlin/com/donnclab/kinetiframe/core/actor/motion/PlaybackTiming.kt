// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

data class PerformanceTiming(val durationMicros: Long, val playback: PerformancePlayback)

enum class PresetClockMode { REPEAT, ONE_SHOT }

data class PresetTiming(val durationMicros: Long, val mode: PresetClockMode)
