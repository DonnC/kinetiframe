// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.round

class PresetDurationTest {
    @Test fun `duration preserves frozen formula including nonfinite and out of range inputs`() {
        val durations = listOf(
            -Float.MAX_VALUE, -1f, -0f, 0f, Float.MIN_VALUE, .25f,
            1.7f, 30f, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
        )
        val speeds = listOf(
            -Float.MAX_VALUE, -1f, -0f, 0f, .24999f, .25f, 1f, 1.3f,
            3f, 3.0001f, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
        )
        durations.forEach { duration ->
            speeds.forEach { speed ->
                val expected = round(duration.toDouble() * 1_000_000 / speed.coerceIn(.25f, 3f))
                    .toLong().coerceAtLeast(1)
                assertEquals(expected, PresetDuration.micros(duration, speed), "$duration/$speed")
            }
        }
    }

    @Test fun `ties round to even rather than roundToLong policy`() {
        assertEquals(7_812L, PresetDuration.micros(.0078125f, 1f))
        assertEquals(23_438L, PresetDuration.micros(.0234375f, 1f))
    }

    @Test fun `speed clamp and minimum duration remain evaluation only`() {
        assertEquals(4_000_000L, PresetDuration.micros(1f, -10f))
        assertEquals(333_333L, PresetDuration.micros(1f, 10f))
        assertEquals(1L, PresetDuration.micros(-1f, 1f))
        assertEquals(1L, PresetDuration.micros(1f, Float.NaN))
        assertEquals(Long.MAX_VALUE, PresetDuration.micros(Float.POSITIVE_INFINITY, 1f))
    }
}
