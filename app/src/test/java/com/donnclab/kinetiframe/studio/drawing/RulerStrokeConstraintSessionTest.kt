package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class RulerStrokeConstraintSessionTest {

    @Test
    fun `line projects input onto an arbitrary axis`() {
        val session = RulerStrokeConstraintSession(spec(mode = RulerConstraintMode.LINE, endX = 10f, endY = 10f))

        val constrained = session.constrain(TouchPoint(10f, 0f))

        assertEquals(5f, constrained.x, TOLERANCE)
        assertEquals(5f, constrained.y, TOLERANCE)
    }

    @Test
    fun `ellipse projection honours guide rotation`() {
        val session = RulerStrokeConstraintSession(
            spec(mode = RulerConstraintMode.ELLIPSE, radiusX = 20f, radiusY = 10f, rotationDegrees = 90f),
        )

        val constrained = session.constrain(TouchPoint(0f, 30f))

        assertEquals(0f, constrained.x, TOLERANCE)
        assertEquals(20f, constrained.y, TOLERANCE)
    }

    @Test
    fun `rectangle retains its initial edge within hysteresis`() {
        val session =
            RulerStrokeConstraintSession(spec(mode = RulerConstraintMode.RECTANGLE, radiusX = 50f, radiusY = 50f))

        val first = session.constrain(TouchPoint(49f, 10f))
        val nearCorner = session.constrain(TouchPoint(45f, 47f))

        assertEquals(50f, first.x, TOLERANCE)
        assertEquals(50f, nearCorner.x, TOLERANCE)
        assertEquals(47f, nearCorner.y, TOLERANCE)
    }

    @Test
    fun `reflection uses the complete diagonal axis`() {
        val session = RulerStrokeConstraintSession(spec(mode = RulerConstraintMode.MIRROR, endX = 10f, endY = 10f))

        val reflected = session.reflect(TouchPoint(10f, 0f))

        assertEquals(0f, reflected.x, TOLERANCE)
        assertEquals(10f, reflected.y, TOLERANCE)
    }

    private fun spec(
        mode: RulerConstraintMode,
        endX: Float = 10f,
        endY: Float = 0f,
        radiusX: Float = 10f,
        radiusY: Float = 10f,
        rotationDegrees: Float = 0f,
    ) = RulerConstraintSpec(
        mode = mode,
        startX = 0f,
        startY = 0f,
        endX = endX,
        endY = endY,
        centerX = 0f,
        centerY = 0f,
        radiusX = radiusX,
        radiusY = radiusY,
        rotationDegrees = rotationDegrees,
    )

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
