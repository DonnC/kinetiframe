package com.donnclab.kinetiframe.studio.ui.canvas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StylusPointerSessionTest {
    @Test fun `stylus contact can begin after ignored palm and keeps its own tap time`() {
        val session = ActivePointerSession()
        // An ignored palm never becomes the drawing pointer.
        assertFalse(session.hasActivePointer)
        session.begin(pointerId = 7, eventTime = 150L)
        assertEquals(150L, session.startTime)
        assertTrue(session.shouldIgnorePointerUp(3))
        assertFalse(session.end(3))
        assertTrue(session.isActive(7))
        assertTrue(session.end(7))
        assertEquals(0L, session.startTime)
    }
}
