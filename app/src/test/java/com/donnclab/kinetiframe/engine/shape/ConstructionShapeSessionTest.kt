package com.donnclab.kinetiframe.engine.shape

import com.donnclab.kinetiframe.engine.model.TouchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionShapeSessionTest {
    @Test fun `capsule previews then completes one generated stroke`() {
        val session = ConstructionShapeSession(ConstructionShape.CAPSULE)
        session.begin(TouchPoint(0f, 0f))
        session.update(TouchPoint(100f, 40f))
        assertEquals(1, session.preview()!!.size)
        assertEquals(1, session.complete()!!.size)
        assertNull(session.preview())
    }

    @Test fun `arc previews during a one drag gesture and completes`() {
        val session = ConstructionShapeSession(ConstructionShape.THREE_POINT_ARC)
        session.begin(TouchPoint(0f, 0f))
        session.update(TouchPoint(100f, 0f))
        assertTrue(session.preview()!!.single().any { it.y != 0f })
        val completed = session.complete()!!
        assertEquals(1, completed.size)
        assertEquals(0f, completed.single().first().x, 0.001f)
        assertEquals(100f, completed.single().last().x, 0.001f)
        assertNull(session.preview())
    }

    @Test fun `arc explicit peak overrides the deterministic one drag fallback`() {
        val session = ConstructionShapeSession(ConstructionShape.THREE_POINT_ARC)
        session.begin(TouchPoint(0f, 0f))
        session.update(TouchPoint(100f, 0f))
        assertTrue(session.confirmPeak(TouchPoint(50f, 60f)))
        session.update(TouchPoint(100f, 0f))
        assertTrue(session.preview()!!.single().minOf { it.y } >= 0f)
    }

    @Test fun `arc preview follows the end point after peak is fixed`() {
        val session = ConstructionShapeSession(ConstructionShape.THREE_POINT_ARC)
        session.begin(TouchPoint(0f, 0f))
        assertTrue(session.confirmPeak(TouchPoint(50f, -40f)))

        session.update(TouchPoint(80f, 20f))
        val preview = session.preview()!!.single()
        assertEquals(80f, preview.last().x, 0.001f)
        assertEquals(20f, preview.last().y, 0.001f)
    }

    @Test fun `arc does not commit a zero length gesture`() {
        val session = ConstructionShapeSession(ConstructionShape.THREE_POINT_ARC)
        session.begin(TouchPoint(20f, 20f))
        session.confirmPeak(TouchPoint(20f, 0f))
        session.update(TouchPoint(20f, 20f))
        assertNull(session.complete())
    }
}
