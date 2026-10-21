package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.engine.model.TouchPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PenInputResponseTest {
    @Test fun `held contact keeps smoothing active until the nib settles`() {
        val stabilizer = MassSpringStabilizer().apply { configureSmoothing(0.5f) }
        stabilizer.reset(TouchPoint(0f, 0f))
        val target = TouchPoint(10f, 0f)
        stabilizer.update(target, 0.016f)
        assertTrue(stabilizer.needsUpdate(target))
        repeat(120) { stabilizer.update(target, 0.016f) }
        assertFalse(stabilizer.needsUpdate(target))
        stabilizer.configureSmoothing(0f)
        assertFalse(stabilizer.needsUpdate(target.copy(x = 50f)))
    }

    @Test fun `custom selection damping remains stable with exact elapsed time`() {
        for (damping in listOf(5f, 80f)) {
            val whole = MassSpringStabilizer(400f, damping, 1f)
            val split = MassSpringStabilizer(400f, damping, 1f)
            val start = TouchPoint(0f, 0f)
            val target = TouchPoint(100f, 0f)
            whole.reset(start)
            split.reset(start)
            val result = whole.update(target, 0.2f)
            var pieces = start
            repeat(20) { pieces = split.update(target, 0.01f) }
            assertTrue(result.x.isFinite())
            assertEquals(result.x, pieces.x, 0.002f)
        }
    }

    @Test fun `zero smoothing is exact raw input including velocity`() {
        val stabilizer = MassSpringStabilizer()
        stabilizer.configureSmoothing(0f)
        stabilizer.reset(TouchPoint(0f, 0f, timestamp = 0))
        val target = TouchPoint(10f, 5f, velocity = 700f, timestamp = 16)
        assertEquals(target, stabilizer.update(target, 0.016f))
    }

    @Test fun `delayed batches remain stable and do not truncate elapsed time`() {
        val whole = MassSpringStabilizer().apply { configureSmoothing(0.5f) }
        val split = MassSpringStabilizer().apply { configureSmoothing(0.5f) }
        val start = TouchPoint(0f, 0f)
        val target = TouchPoint(100f, 100f, velocity = 123f)
        whole.reset(start)
        split.reset(start)
        val result = whole.update(target, 0.2f)
        var pieces = start
        repeat(20) { pieces = split.update(target, 0.01f) }
        assertEquals(result.x, pieces.x, 0.001f)
        assertTrue(result.x in 0f..100f)
        assertEquals(123f, result.velocity)
    }

    @Test fun `pressure response agrees at 60 120 and 240 Hz`() {
        fun response(rate: Int): Float {
            var pressure = 0.2f
            repeat(rate / 5) { pressure = PressureResponse.filter(pressure, 0.9f, 1f / rate) }
            return pressure
        }
        assertEquals(response(60), response(120), 0.0001f)
        assertEquals(response(60), response(240), 0.0001f)
    }

    @Test fun `stationary force and tilt changes survive noise filtering`() {
        val start = TouchPoint(10f, 10f, pressure = 0.2f, timestamp = 1)
        assertTrue(InputFilter.adaptiveThreshold(start.copy(pressure = 0.5f, timestamp = 17), start))
        assertTrue(InputFilter.adaptiveThreshold(start.copy(tilt = 0.2f, timestamp = 17), start))
        assertFalse(InputFilter.adaptiveThreshold(start.copy(timestamp = 17), start))
    }

    @Test fun `finger pressure is stable and invalid stylus force is safe`() {
        assertEquals(1f, PressureResponse.contactPressure(0.1f, false))
        assertEquals(1f, PressureResponse.contactPressure(0.9f, false))
        assertEquals(1f, PressureResponse.contactPressure(Float.NaN, true))
        assertEquals(0.4f, PressureResponse.contactPressure(0.4f, true))
        assertEquals(0.5f, PressureResponse.filter(0.5f, Float.NaN, 0.016f))
    }
}
