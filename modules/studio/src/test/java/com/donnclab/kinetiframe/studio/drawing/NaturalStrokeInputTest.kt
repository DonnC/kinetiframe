package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class NaturalStrokeInputTest {
    @Test fun held_nib_ignores_position_force_and_angle_noise() {
        val input = NaturalStrokeInput()
        val first = TouchPoint(20f, 30f, pressure = 0.5f, timestamp = 0)
        input.update(first, first, true)
        repeat(1000) { i ->
            val p = first.copy(
                x = 20f + if (i % 2 == 0) 0.4f else -0.4f,
                y = 30f + if (i % 3 == 0) 0.3f else -0.3f,
                pressure = 0.5f + if (i % 2 == 0) 0.025f else -0.025f,
                tilt = i * 0.01f,
                timestamp = i + 1L,
            )
            val out = requireNotNull(input.update(p, p, true))
            assertEquals(20f, out.x)
            assertEquals(30f, out.y)
            assertEquals(0.5f, out.pressure, 0.0001f)
        }
    }

    @Test fun noisy_straight_lines_are_filtered_without_long_trailing_lag() {
        for (hz in listOf(60, 120, 240)) {
            val input = NaturalStrokeInput()
            var squaredError = 0f
            var last = TouchPoint(0f, 0f)
            for (i in 0..hz * 2) {
                val p = TouchPoint(
                    i * 240f / hz,
                    if (i == 0) {
                        0f
                    } else if (i % 2 == 0) {
                        1.5f
                    } else {
                        -1.5f
                    },
                    timestamp = i * 1000L / hz,
                )
                last = requireNotNull(input.update(p, p, true))
                squaredError += last.y * last.y
            }
            assertTrue(sqrt(squaredError / (hz * 2 + 1)) < 0.65f, "noise at $hz Hz")
            assertTrue(480f - last.x in 0f..5f, "lag at $hz Hz: ${480f - last.x}")
        }
    }

    @Test fun slow_motion_is_continuous_not_quantized() {
        val input = NaturalStrokeInput()
        var previous = 0f
        var emitted = 0
        repeat(400) { i ->
            val p = TouchPoint(i * 0.05f, 0f, timestamp = i * 8L)
            val out = requireNotNull(input.update(p, p, false))
            assertTrue(out.x >= previous && out.x - previous < 0.1f)
            if (out.x > previous) emitted++
            previous = out.x
        }
        assertTrue(emitted > 350)
        assertTrue(previous > 19f)
    }

    @Test fun circles_follow_the_curve_without_spline_overshoot() {
        val input = NaturalStrokeInput()
        repeat(481) { i ->
            val angle = i * 2f * PI.toFloat() / 240
            val p = TouchPoint(100f * cos(angle), 100f * sin(angle), timestamp = i * 1000L / 120)
            val out = requireNotNull(input.update(p, p, true))
            if (i > 20) assertTrue(hypot(out.x, out.y) in 97f..100.01f)
        }
    }

    @Test fun filtering_is_affine_invariant_under_canvas_zoom_and_rotation() {
        val screenInput = NaturalStrokeInput()
        val documentInput = NaturalStrokeInput()
        fun transform(p: TouchPoint) = p.copy(x = -p.y * 4 + 100, y = p.x * 4 - 50)
        repeat(300) { i ->
            val p = TouchPoint(i.toFloat(), sin(i / 8f) * 12, timestamp = i * 8L)
            val expected = transform(requireNotNull(screenInput.update(p, p, true)))
            val actual = requireNotNull(documentInput.update(transform(p), p, true))
            assertEquals(expected.x, actual.x, 0.001f)
            assertEquals(expected.y, actual.y, 0.001f)
        }
    }

    @Test fun duplicates_invalid_samples_and_reset_do_not_poison_input() {
        val input = NaturalStrokeInput()
        val p = TouchPoint(5f, 8f, timestamp = 10)
        assertNotNull(input.update(p, p, true))
        assertNull(input.update(p, p, true))
        assertNull(input.update(p.copy(timestamp = 9), p, true))
        assertNull(input.update(p.copy(x = Float.NaN, timestamp = 11), p, true))
        input.reset()
        assertEquals(5f, input.update(p, p, true)?.x)
    }

    @Test fun finger_has_constant_force_while_intentional_stylus_force_survives() {
        for (stylus in listOf(false, true)) {
            val input = NaturalStrokeInput()
            val first = TouchPoint(1f, 1f, pressure = 0.2f, timestamp = 0)
            input.update(first, first, stylus)
            var result = first
            repeat(60) { i ->
                val p = first.copy(pressure = 0.9f, timestamp = (i + 1) * 16L)
                result = requireNotNull(input.update(p, p, stylus))
            }
            assertEquals(if (stylus) 0.9f else 1f, result.pressure, 0.006f)
        }
    }

    @Test fun grouping_historical_samples_does_not_change_the_result() {
        val points = List(200) { TouchPoint(it.toFloat(), sin(it / 10f), timestamp = it * 8L) }
        fun process(groups: List<List<TouchPoint>>): List<TouchPoint> {
            val input = NaturalStrokeInput()
            return groups.flatMap { batch -> batch.map { requireNotNull(input.update(it, it, true)) } }
        }
        assertEquals(process(listOf(points)), process(points.chunked(7)))
    }

    @Test fun seeded_sensor_noise_is_reduced_during_handwriting() {
        for (hz in listOf(60, 120, 240)) {
            val input = NaturalStrokeInput()
            val random = kotlin.random.Random(8)
            var error = 0f
            for (i in 0..hz * 4) {
                val p = TouchPoint(
                    i * 180f / hz,
                    if (i == 0) 0f else random.nextFloat() * 3f - 1.5f,
                    timestamp = i * 1000L / hz,
                )
                val out = requireNotNull(input.update(p, p, true))
                error += out.y * out.y
            }
            assertTrue(sqrt(error / (hz * 4 + 1)) < 0.65f)
        }
    }

    @Test fun density_changes_scale_the_hold_radius_not_the_document() {
        val input = NaturalStrokeInput().apply { reset(3f) }
        val origin = TouchPoint(0f, 0f, timestamp = 0)
        input.update(origin, origin, true)
        val p = TouchPoint(1.8f, 0f, timestamp = 16)
        assertEquals(0f, input.update(p, p, true)?.x)
        val move = p.copy(x = 8f, timestamp = 32)
        assertTrue(requireNotNull(input.update(move, move, true)).x > 0f)
    }
}
