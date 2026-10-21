package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.engine.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class PhysicsEngineTests {

    @Test
    fun `JitterRejectionTest - filter filters high frequency noise`() {
        val filter = MassSpringStabilizer(stiffness = 500f, damping = 45f, mass = 1.0f)
        val startPoint = TouchPoint(100f, 100f, timestamp = 0L)
        filter.reset(startPoint)

        val random = Random(42)
        var lastOutput = startPoint

        // Feed 120Hz random noise within 0.8px for 1 second
        for (i in 1..120) {
            val noiseX = (random.nextFloat() - 0.5f) * 1.6f // -0.8 to 0.8
            val noiseY = (random.nextFloat() - 0.5f) * 1.6f
            val target = TouchPoint(100f + noiseX, 100f + noiseY, timestamp = i * 8L) // ~120Hz

            lastOutput = filter.update(target, 0.00833f)
        }

        // Assert the output is stable
        assertEquals(100f, lastOutput.x, 0.5f)
        assertEquals(100f, lastOutput.y, 0.5f)
    }

    @Test
    fun `StabilizerPhysicsTest - Physics filters better than EMA`() {
        // High damping and moderate stiffness for superior stability over EMA
        val stiffness = 400f
        val damping = 80f
        val mass = 1.0f

        val stabilizer = MassSpringStabilizer(stiffness, damping, mass)
        val startPoint = TouchPoint(0f, 0f, timestamp = 0L)
        stabilizer.reset(startPoint)

        // EMA for comparison
        var emaX = 0f
        var emaY = 0f
        val alpha = 0.2f // Responsive EMA

        val random = Random(1337)
        var physicsTotalDeviation = 0f
        var emaTotalDeviation = 0f

        // First move to 10,10 to settle
        for (i in 1..50) {
            stabilizer.update(TouchPoint(10f, 10f), 0.016f)
            emaX = emaX + alpha * (10f - emaX)
            emaY = emaY + alpha * (10f - emaY)
        }

        for (i in 1..100) {
            val noise = (random.nextFloat() - 0.5f) * 2f
            val target = TouchPoint(10f + noise, 10f + noise, timestamp = (i + 50) * 16L)

            val physicsOut = stabilizer.update(target, 0.016f)

            emaX = emaX + alpha * (target.x - emaX)
            emaY = emaY + alpha * (target.y - emaY)

            physicsTotalDeviation += hypot(physicsOut.x - 10f, physicsOut.y - 10f)
            emaTotalDeviation += hypot(emaX - 10f, emaY - 10f)
        }

        // Physics-based smoothing should be more stable (less deviation from the ideal 10,10)
        assertTrue(
            "Physics ($physicsTotalDeviation) should be more stable than EMA ($emaTotalDeviation)",
            physicsTotalDeviation < emaTotalDeviation,
        )
    }

    @Test
    fun `InputFilter - AdaptiveThreshold works`() {
        val p1 = TouchPoint(0f, 0f, timestamp = 0L)
        val p2 = TouchPoint(0.3f, 0.3f, timestamp = 100L) // Small movement, low velocity

        assertFalse(
            "Noise below 0.5px should be rejected at low velocity",
            InputFilter.adaptiveThreshold(p2, p1),
        )

        val p3 = TouchPoint(10f, 10f, timestamp = 10L) // Large movement, high velocity
        assertTrue(
            "Large movement should be accepted",
            InputFilter.adaptiveThreshold(p3, p1),
        )
    }

    @Test
    fun `InputFilter - DouglasPeucker simplifies path`() {
        val points = listOf(
            TouchPoint(0f, 0f),
            TouchPoint(5f, 0.1f),
            TouchPoint(10f, 0f),
        )

        val simplified = InputFilter.simplifyDouglasPeucker(points, 0.5f)
        assertEquals(2, simplified.size) // Should remove the middle point
        assertEquals(0f, simplified[0].x)
        assertEquals(10f, simplified[1].x)
    }

    @Test
    fun `Stabilizer - clear() allows fresh initialization`() {
        val stabilizer = MassSpringStabilizer()
        val firstPoint = TouchPoint(100f, 100f)

        // Simulate previous state
        stabilizer.reset(TouchPoint(50f, 50f))

        // Clear for the new stroke
        stabilizer.clear()

        val result = stabilizer.update(firstPoint, 0.016f)

        // It should be exactly the first point, not springing from (50,50) or (0,0)
        assertEquals(100f, result.x, 0.001f)
        assertEquals(100f, result.y, 0.001f)
    }

    @Test
    fun `Stabilizer - automatic initialization on first update`() {
        val stabilizer = MassSpringStabilizer()
        val firstPoint = TouchPoint(100f, 100f)

        // When uninitialized, the first update should return the target point directly
        val result = stabilizer.update(firstPoint, 0.016f)

        assertEquals(100f, result.x, 0.001f)
        assertEquals(100f, result.y, 0.001f)
    }

    @Test
    fun `Stabilizer - default response does not overshoot a stationary target`() {
        val stabilizer = MassSpringStabilizer()
        stabilizer.reset(TouchPoint(0f, 0f, timestamp = 0L))

        var largestX = 0f
        repeat(120) { index ->
            val output = stabilizer.update(TouchPoint(100f, 0f, timestamp = (index + 1L) * 8L), 0.008f)
            largestX = maxOf(largestX, output.x)
        }

        assertTrue("Critically damped defaults must not overshoot the target", largestX <= 100.01f)
    }

    @Test
    fun `InputFilter - backwards timestamp cannot inflate velocity`() {
        val previous = TouchPoint(0f, 0f, timestamp = 100L)
        val backwards = TouchPoint(0.3f, 0.3f, timestamp = 90L)

        assertFalse(InputFilter.adaptiveThreshold(backwards, previous))
    }
}
