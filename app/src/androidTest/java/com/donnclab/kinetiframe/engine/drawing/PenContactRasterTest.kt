package com.donnclab.kinetiframe.engine.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.ui.components.canvas.ActiveStroke
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** These contracts require Android's real Path/Canvas rasterizer, not JVM graphics stubs. */
class PenContactRasterTest {
    @Test fun held_sensor_noise_does_not_grow_a_stroke() {
        val stroke = ActiveStroke().apply { configureInput() }
        stroke.appendSample(TouchPoint(48f, 48f, pressure = 0.5f, timestamp = 0))
        repeat(1000) { i ->
            val point = TouchPoint(
                48f + if (i % 2 == 0) 0.4f else -0.4f,
                48f + if (i % 3 == 0) 0.3f else -0.3f,
                pressure = 0.5f + if (i % 2 == 0) 0.025f else -0.025f,
                tilt = i * 0.01f,
                timestamp = i + 1L,
            )
            stroke.appendSample(point)
        }
        assertEquals(1, stroke.points.size)
        val preview = pixels { stroke.drawIncrementalPen(it, 10f, paint()) }
        stroke.flushSample(TouchPoint(48.3f, 48.2f, pressure = 0f, timestamp = 1001))
        assertEquals(1, stroke.points.size)
        assertArrayEquals(preview, pixels { PenStrokeRenderer.draw(it, stroke.points, 10f, paint()) })
    }

    @Test fun stationary_force_response_continues_until_settled() {
        val stroke = ActiveStroke().apply { configureInput(pressureSensitive = true) }
        stroke.appendSample(TouchPoint(48f, 48f, pressure = 0.2f, timestamp = 1))
        repeat(60) { index ->
            stroke.appendSample(TouchPoint(48f, 48f, pressure = 0.9f, timestamp = 17L + index * 16L))
        }
        assertEquals(0.9f, stroke.points.last().pressure, 0.006f)
    }
    private fun pixels(draw: (Canvas) -> Unit): IntArray {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        return try {
            draw(Canvas(bitmap))
            IntArray(96 * 96).also { bitmap.getPixels(it, 0, 96, 0, 0, 96, 96) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun paint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 128
    }

    @Test fun stationary_contacts_leave_the_same_visible_disk_at_any_sample_count() {
        val point = TouchPoint(48f, 48f, pressure = 0.5f, timestamp = 0L)
        val expected = pixels { PressureStrokeRenderer.draw(it, listOf(point), 10f, paint()) }
        assertTrue(Color.alpha(expected[48 * 96 + 48]) > 0)
        for (count in 2..10) {
            val samples = List(count) { point.copy(timestamp = it.toLong()) }
            assertArrayEquals(expected, pixels { PressureStrokeRenderer.draw(it, samples, 10f, paint()) })
        }
    }

    @Test fun retained_preview_and_completed_stroke_have_identical_pixels_including_alpha() {
        val stroke = ActiveStroke().apply { configureInput() }
        val samples = listOf(
            TouchPoint(15f, 30f, timestamp = 1),
            TouchPoint(40f, 20f, timestamp = 17),
            TouchPoint(65f, 50f, timestamp = 33),
            TouchPoint(40f, 70f, timestamp = 49),
            TouchPoint(35f, 30f, timestamp = 65),
            TouchPoint(60f, 40f, timestamp = 81),
        )
        samples.forEach { sample ->
            stroke.appendSample(sample)
            assertArrayEquals(
                pixels { PenStrokeRenderer.draw(it, stroke.points, 10f, paint()) },
                pixels { stroke.drawIncrementalPen(it, 10f, paint()) },
            )
        }
        val preview = pixels { stroke.drawIncrementalPen(it, 10f, paint()) }
        val completed = pixels { PenStrokeRenderer.draw(it, stroke.points, 10f, paint()) }
        assertArrayEquals(completed, preview)
        assertTrue(preview.maxOf { Color.alpha(it) } <= 128)
    }

    @Test fun finger_width_and_raw_speed_are_independent_of_document_zoom() {
        fun stroke(scale: Float): ActiveStroke = ActiveStroke().apply {
            configureInput()
            isStylusContact = false
            appendSample(TouchPoint(0f, 0f, pressure = 0.1f, timestamp = 1), TouchPoint(0f, 0f, timestamp = 1))
            appendSample(
                TouchPoint(10f * scale, 0f, pressure = 0.9f, timestamp = 17),
                TouchPoint(10f, 0f, timestamp = 17),
            )
        }
        val first = stroke(1f).points.last()
        val zoomed = stroke(4f).points.last()
        assertEquals(1f, first.pressure, 0f)
        assertEquals(first.velocity, zoomed.velocity, 0f)
    }

    @Test fun pen_width_is_independent_of_pressure_and_speed() {
        val points = listOf(TouchPoint(10f, 20f), TouchPoint(45f, 40f), TouchPoint(80f, 70f))
        val expected = pixels { PenStrokeRenderer.draw(it, points, 6f, paint()) }
        assertArrayEquals(
            expected,
            pixels {
                PenStrokeRenderer.draw(
                    it,
                    points.mapIndexed { i, p -> p.copy(pressure = i * 0.15f, velocity = i * 2000f) },
                    6f,
                    paint(),
                )
            },
        )
    }

    @Test fun repeated_identical_pen_contacts_are_still_a_visible_dot() {
        val point = TouchPoint(48f, 48f, timestamp = 0)
        val expected = pixels { PenStrokeRenderer.draw(it, listOf(point), 10f, paint()) }
        for (count in 2..10) {
            assertArrayEquals(
                expected,
                pixels {
                    PenStrokeRenderer.draw(it, List(count) { point.copy(timestamp = it.toLong()) }, 10f, paint())
                },
            )
        }
    }

    @Test fun only_explicit_bake_receipts_remove_pending_ink() {
        val stroke = ActiveStroke()
        val point = TouchPoint(48f, 48f, timestamp = 100)
        for (seed in listOf(100L, 200L)) {
            stroke.settle(
                listOf(point.copy(timestamp = seed)),
                com.donnclab.kinetiframe.engine.model.DrawingTool.BRUSH,
                com.donnclab.kinetiframe.ui.EraserType.AREA,
                6f,
                1f,
                paint(),
                null,
                BrushSnapshot(seed = seed),
            )
        }
        stroke.acknowledgeSettledStrokes(setOf(999L))
        assertEquals(2, stroke.settledStrokes.size)
        stroke.acknowledgeSettledStrokes(setOf(200L))
        assertEquals(listOf(100L), stroke.settledStrokes.map { it.brush.seed })
        stroke.acknowledgeSettledStrokes(setOf(100L, 200L))
        assertTrue(stroke.settledStrokes.isEmpty())
    }
}
