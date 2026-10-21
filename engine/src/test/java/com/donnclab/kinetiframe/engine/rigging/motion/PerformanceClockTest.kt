// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.PerformanceTiming
import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PerformanceClockTest {
    @Test fun `document loops floor wrap negative and extreme times`() {
        val timing = PerformanceTiming(1000, PerformancePlayback.LOOP)
        listOf(Long.MIN_VALUE, -1001, -1, 0, 999, 1000, 1001, Long.MAX_VALUE).forEach { time ->
            assertEquals(Math.floorMod(time, 1000L), PerformanceClock.normalize(timing, time))
        }
    }

    @Test fun `one shot and end hold clamp to document duration`() {
        listOf(PerformancePlayback.ONE_SHOT, PerformancePlayback.END_HOLD).forEach { mode ->
            val timing = PerformanceTiming(1000, mode)
            assertEquals(0L, PerformanceClock.normalize(timing, Long.MIN_VALUE))
            assertEquals(500L, PerformanceClock.normalize(timing, 500))
            assertEquals(1000L, PerformanceClock.normalize(timing, Long.MAX_VALUE))
        }
    }

    @Test fun `standalone repeating presets use their own period and clamp negative input`() {
        val timing = PerformanceTiming(1000, PerformancePlayback.LOOP)
        val preset = PresetTiming(800, PresetClockMode.REPEAT)
        assertEquals(0L, PerformanceClock.normalize(timing, -1, preset))
        assertEquals(200L, PerformanceClock.normalize(timing, 1000, preset))
        assertEquals(0L, PerformanceClock.normalize(timing, 1600, preset))
    }

    @Test fun `standalone one shot presets hold their own end for every container mode`() {
        val preset = PresetTiming(800, PresetClockMode.ONE_SHOT)
        PerformancePlayback.entries.forEach { mode ->
            val timing = PerformanceTiming(1000, mode)
            assertEquals(0L, PerformanceClock.normalize(timing, -1, preset))
            assertEquals(800L, PerformanceClock.normalize(timing, Long.MAX_VALUE, preset))
        }
    }

    @Test fun `nonloop containers retain their duration for repeating preset clocks`() {
        val preset = PresetTiming(800, PresetClockMode.REPEAT)
        listOf(PerformancePlayback.ONE_SHOT, PerformancePlayback.END_HOLD).forEach { mode ->
            assertEquals(1000L, PerformanceClock.normalize(PerformanceTiming(1000, mode), 1600, preset))
        }
    }

    @Test fun `raw invalid durations keep previous arithmetic and range failures`() {
        assertThrows(ArithmeticException::class.java) {
            PerformanceClock.normalize(PerformanceTiming(0, PerformancePlayback.LOOP), 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PerformanceClock.normalize(PerformanceTiming(-1, PerformancePlayback.END_HOLD), 1)
        }
        assertEquals(0L, PerformanceClock.normalize(PerformanceTiming(0, PerformancePlayback.ONE_SHOT), 1))
    }
}
