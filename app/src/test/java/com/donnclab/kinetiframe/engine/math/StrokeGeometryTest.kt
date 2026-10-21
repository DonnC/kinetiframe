package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.engine.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeGeometryTest {

    @Test
    fun `adaptive tessellation keeps each straight render step within policy`() {
        val start = TouchPoint(0f, 0f)
        val end = TouchPoint(80f, 0f)

        assertEquals(20, StrokeGeometry.interpolationSteps(start, end))
    }

    @Test
    fun `adaptive tessellation does not allocate excessive segments for a tap`() {
        val point = TouchPoint(20f, 20f)

        assertEquals(1, StrokeGeometry.interpolationSteps(point, point))
    }

    @Test
    fun `interpolated segments preserve their source endpoints`() {
        val p0 = TouchPoint(0f, 0f)
        val p1 = TouchPoint(10f, 5f)
        val p2 = TouchPoint(30f, 20f)
        val p3 = TouchPoint(45f, 25f)

        val result = StrokeGeometry.interpolatedSegment(p0, p1, p2, p3)

        assertEquals(p1.x, result.first().x, 0.001f)
        assertEquals(p1.y, result.first().y, 0.001f)
        assertEquals(p2.x, result.last().x, 0.001f)
        assertEquals(p2.y, result.last().y, 0.001f)
    }

    @Test
    fun `dynamic width incorporates velocity for professional tapering`() {
        val baseWidth = 10f
        val pressure = 0.5f

        val slowWidth = StrokeGeometry.calculateDynamicWidth(baseWidth, pressure, 0f)
        val fastWidth = StrokeGeometry.calculateDynamicWidth(baseWidth, pressure, 3000f)
        val veryFastWidth = StrokeGeometry.calculateDynamicWidth(baseWidth, pressure, 6000f)

        assertTrue("Line should be thinner at high speeds", fastWidth < slowWidth)
        assertEquals("Tapering should be capped at 30%", slowWidth * 0.7f, veryFastWidth, 0.001f)
    }

    @Test
    fun `dynamic width multiplier is monotonic and bounded by pressure`() {
        val baseWidth = 1f
        val low = StrokeGeometry.calculateDynamicWidth(baseWidth, 0f, 0f)
        val high = StrokeGeometry.calculateDynamicWidth(baseWidth, 1f, 0f)

        assertTrue(low < high)
        assertEquals(0.45f, low, 0.001f)
        assertEquals(1.2f, high, 0.001f)
    }

    @Test
    fun `incremental rendering only emits the requested unsettled tail`() {
        val points = listOf(
            TouchPoint(0f, 0f),
            TouchPoint(10f, 0f),
            TouchPoint(20f, 0f),
            TouchPoint(30f, 0f),
        )
        var segmentCount = 0

        StrokeGeometry.forEachSegment(points, startSourceSegment = 2) { _, _, _ ->
            segmentCount++
        }

        // Only the requested tail is emitted; adaptive subdivision may use more than ceil(length/4).
        assertTrue(segmentCount in 3..8)
    }
}
