package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionShapeGeometryTest {
    @Test fun `capsule outline closes without a centre seam`() {
        val stroke = ConstructionShapeGeometry.sample(
            ConstructionShape.CAPSULE,
            ShapeBounds(0f, 0f, 100f, 40f),
        ).single()
        assertEquals(stroke.first().x, stroke.last().x, 0.001f)
        assertEquals(stroke.first().y, stroke.last().y, 0.001f)
    }

    @Test fun `head guide produces perimeter and two construction strokes`() {
        val result = ConstructionShapeGeometry.sample(ConstructionShape.HEAD_GUIDE, ShapeBounds(0f, 0f, 100f, 80f))
        assertEquals(3, result.size)
        assertEquals(50f, result[1].first().x, 0.001f)
        assertEquals(40f, result[2].first().y, 0.001f)
        assertEquals(4, result[1].size)
        assertEquals(4, result[2].size)
    }

    @Test fun `triangle closes at its apex`() {
        val stroke = ConstructionShapeGeometry.sample(
            ConstructionShape.TRIANGLE,
            ShapeBounds(0f, 0f, 100f, 80f),
        ).single()
        assertEquals(stroke.first().x, stroke.last().x, 0.001f)
        assertEquals(stroke.first().y, stroke.last().y, 0.001f)
    }

    @Test fun `arc preserves its start and end points`() {
        val stroke = ConstructionShapeGeometry.sample(
            ConstructionShape.THREE_POINT_ARC,
            ShapeBounds(0f, 0f, 100f, 0f),
            TouchPoint(50f, 80f),
        ).single()
        assertEquals(0f, stroke.first().x, 0.001f)
        assertEquals(100f, stroke.last().x, 0.001f)
    }

    @Test fun `arc bends toward its control peak instead of becoming a straight chord`() {
        val stroke = ConstructionShapeGeometry.sample(
            ConstructionShape.THREE_POINT_ARC,
            ShapeBounds(0f, 0f, 100f, 0f),
            peak = TouchPoint(50f, -80f),
            steps = 10,
        ).single()

        // A quadratic Bezier's midpoint is halfway between the chord and peak.
        assertEquals(50f, stroke[5].x, 0.001f)
        assertEquals(-40f, stroke[5].y, 0.001f)
        assertTrue(stroke.minOf { it.y } < 0f)
    }

    @Test fun `arc sampling is deterministic and contains one point per step`() {
        val stroke = ConstructionShapeGeometry.sample(
            ConstructionShape.THREE_POINT_ARC,
            ShapeBounds(10f, 20f, 90f, 60f),
            peak = TouchPoint(50f, 0f),
            steps = 8,
        ).single()

        assertEquals(9, stroke.size)
        assertTrue(stroke.zipWithNext().all { (a, b) -> a.x != b.x || a.y != b.y })
    }
}
