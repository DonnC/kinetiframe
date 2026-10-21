package com.donnclab.kinetiframe.ui.components.canvas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePointerSessionTest {

    @Test
    fun `adding a palm does not replace the active stylus`() {
        val session = ActivePointerSession()

        session.begin(pointerId = 41)

        assertTrue(session.isActive(41))
        assertFalse(session.isActive(7))
    }

    @Test
    fun `lifting a non-active palm does not end the stylus session`() {
        val session = ActivePointerSession()
        session.begin(pointerId = 41)

        assertTrue(session.shouldIgnorePointerUp(7))
        assertFalse(session.end(7))
        assertTrue(session.hasActivePointer)
        assertTrue(session.isActive(41))
    }

    @Test
    fun `lifting the active stylus ends exactly its own session`() {
        val session = ActivePointerSession()
        session.begin(pointerId = 41)

        assertTrue(session.end(41))
        assertFalse(session.hasActivePointer)
    }

    @Test
    fun `two touch contacts remain available for viewport navigation`() {
        assertTrue(ViewportGesturePolicy.shouldTransform(pressedPointerCount = 2, hasStylusContact = false))
        assertFalse(ViewportGesturePolicy.shouldTransform(pressedPointerCount = 1, hasStylusContact = false))
    }

    @Test
    fun `stylus with a resting palm remains a drawing interaction`() {
        assertFalse(ViewportGesturePolicy.shouldTransform(pressedPointerCount = 2, hasStylusContact = true))
    }
}
