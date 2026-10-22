package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatmullRomSolverTest {

    @Test
    fun `test interpolation produces correct number of points`() {
        val p0 = TouchPoint(0f, 0f)
        val p1 = TouchPoint(10f, 10f)
        val p2 = TouchPoint(20f, 20f)
        val p3 = TouchPoint(30f, 30f)

        val steps = 10
        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps)

        // steps + 1 points (including start and end of segment)
        assertEquals(steps + 1, result.size)
    }

    @Test
    fun `test interpolation endpoints match`() {
        val p0 = TouchPoint(0f, 0f)
        val p1 = TouchPoint(10f, 20f)
        val p2 = TouchPoint(50f, 80f)
        val p3 = TouchPoint(100f, 100f)

        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps = 10)

        // The first point should be p1
        assertEquals(p1.x, result.first().x, 0.01f)
        assertEquals(p1.y, result.first().y, 0.01f)

        // The last point should be p2
        assertEquals(p2.x, result.last().x, 0.01f)
        assertEquals(p2.y, result.last().y, 0.01f)
    }

    @Test
    fun `test pressure interpolation`() {
        val p0 = TouchPoint(0f, 0f, 0.2f)
        val p1 = TouchPoint(10f, 10f, 0.4f)
        val p2 = TouchPoint(20f, 20f, 0.6f)
        val p3 = TouchPoint(30f, 30f, 0.8f)

        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps = 10)

        // Midpoint should have intermediate pressure
        val midPoint = result[5]
        assertTrue(midPoint.pressure > 0.4f)
        assertTrue(midPoint.pressure < 0.6f)
    }

    @Test
    fun `test tilt interpolation`() {
        val p0 = TouchPoint(0f, 0f, 1f, 0.1f)
        val p1 = TouchPoint(10f, 10f, 1f, 0.2f)
        val p2 = TouchPoint(20f, 20f, 1f, 0.3f)
        val p3 = TouchPoint(30f, 30f, 1f, 0.4f)

        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps = 10)

        // Midpoint should have intermediate tilt
        val midPoint = result[5]
        assertTrue(midPoint.tilt > 0.2f)
        assertTrue(midPoint.tilt < 0.3f)
    }

    @Test
    fun `test orientation interpolation`() {
        val p0 = TouchPoint(0f, 0f, 1f, 0f, 0.1f)
        val p1 = TouchPoint(10f, 10f, 1f, 0f, 0.2f)
        val p2 = TouchPoint(20f, 20f, 1f, 0f, 0.3f)
        val p3 = TouchPoint(30f, 30f, 1f, 0f, 0.4f)

        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps = 10)

        // Midpoint should have intermediate orientation
        val midPoint = result[5]
        assertTrue(midPoint.orientation > 0.2f)
        assertTrue(midPoint.orientation < 0.3f)
    }

    @Test
    fun `test velocity interpolation`() {
        val p0 = TouchPoint(0f, 0f, 1f, 0f, 0f, 100f)
        val p1 = TouchPoint(10f, 10f, 1f, 0f, 0f, 200f)
        val p2 = TouchPoint(20f, 20f, 1f, 0f, 0f, 400f)
        val p3 = TouchPoint(30f, 30f, 1f, 0f, 0f, 500f)

        val result = CatmullRomSolver.interpolate(p0, p1, p2, p3, steps = 10)

        // Midpoint should have intermediate velocity
        val midPoint = result[5]
        assertEquals(300f, midPoint.velocity, 0.001f)
    }

    @Test
    fun `test bezier control points calculation`() {
        // Use uniform spacing for easier verification
        val p0 = TouchPoint(0f, 0f)
        val p1 = TouchPoint(10f, 0f)
        val p2 = TouchPoint(20f, 0f)
        val p3 = TouchPoint(30f, 0f)

        val cp = CatmullRomSolver.calculateBezierControlPoints(p0, p1, p2, p3)

        // With uniform spacing, centripetal matches uniform Catmull-Rom
        // CP1 = P1 + (P2 - P0) / 6 = 10 + 20/6 = 13.333
        assertEquals(13.333f, cp.cp1X, 0.01f)
        assertEquals(0f, cp.cp1Y, 0.01f)

        // CP2 = P2 - (P3 - P1) / 6 = 20 - 20/6 = 16.666
        assertEquals(16.666f, cp.cp2X, 0.01f)
        assertEquals(0f, cp.cp2Y, 0.01f)
    }
}
