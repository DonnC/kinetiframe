// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompiledMotionCurveTest {
    @Test fun `sampling retains holds endpoints and authored keys`() {
        val curve = CompiledMotionCurve(listOf(0, 100, 200), listOf(0f, 40f, 40f))
        assertEquals(0f, curve.sample(-1))
        assertEquals(40f, curve.sample(100))
        assertEquals(40f, curve.sample(150))
        assertEquals(40f, curve.sample(201))
        assertEquals(0f, curve.sample(50, CompiledMotionCurve.Interpolation.STEP))
        assertEquals(20f, curve.sample(50, CompiledMotionCurve.Interpolation.LINEAR))
    }

    @Test fun `smooth sampling does not overshoot an authored extremum`() {
        val curve = CompiledMotionCurve(listOf(0, 100, 200, 300), listOf(0f, 80f, -20f, 0f), loop = true)
        for (time in 0L..300L) assertTrue(curve.sample(time) in -20f..80f)
    }

    @Test fun `angular sampling takes shortest arc and retains wrap endpoint`() {
        val curve = CompiledMotionCurve(listOf(0, 100), listOf(170f, -170f), angular = true)
        assertEquals(180f, curve.sample(50, CompiledMotionCurve.Interpolation.LINEAR))
        assertEquals(-170f, curve.sample(100))
        assertEquals(-20f, CompiledMotionCurve.shortestAngle(340f))
    }

    @Test fun `compiled channel owns copied inputs and rejects malformed data`() {
        val times = mutableListOf(0L, 100L)
        val values = mutableListOf(0f, 40f)
        val curve = CompiledMotionCurve(times, values)
        times[1] = 1L
        values[1] = -80f
        assertEquals(20f, curve.sample(50, CompiledMotionCurve.Interpolation.LINEAR))
        assertThrows(IllegalArgumentException::class.java) { CompiledMotionCurve(listOf(0, 0), listOf(0f, 1f)) }
        assertThrows(IllegalArgumentException::class.java) { CompiledMotionCurve(listOf(0), listOf(Float.NaN)) }
    }
}
