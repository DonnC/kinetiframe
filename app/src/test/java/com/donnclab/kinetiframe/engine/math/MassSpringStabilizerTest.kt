package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.engine.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MassSpringStabilizerTest {

    @Test
    fun `high smoothing reduces stiffness and results in leash effect`() {
        val stabilizer = MassSpringStabilizer()

        stabilizer.configureSmoothing(0f)
        val stiffK = stabilizer.stiffness

        stabilizer.configureSmoothing(1f)
        val looseK = stabilizer.stiffness

        assertTrue("High smoothing should have lower stiffness", looseK < stiffK)
    }

    @Test
    fun `stabilizer maintains critical damping when smoothing changes`() {
        val stabilizer = MassSpringStabilizer(mass = 1.0f)

        stabilizer.configureSmoothing(0.5f)
        val k = stabilizer.stiffness
        val c = stabilizer.damping

        // Critical damping c = 2 * sqrt(k * m)
        val criticalC = 2f * kotlin.math.sqrt(k * 1.0f)

        assertEquals(criticalC, c, 0.1f)
    }

    @Test
    fun `update preserves real contact velocity`() {
        val stabilizer = MassSpringStabilizer()
        val start = TouchPoint(0f, 0f, timestamp = 0L)
        val next = TouchPoint(100f, 100f, velocity = 42f, timestamp = 16L)

        stabilizer.reset(start)
        val result = stabilizer.update(next, 0.016f)

        assertEquals(42f, result.velocity, 0f)
    }
}
