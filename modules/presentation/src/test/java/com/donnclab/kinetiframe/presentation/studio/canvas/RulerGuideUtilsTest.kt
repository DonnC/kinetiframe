package com.donnclab.kinetiframe.presentation.studio.canvas

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RulerGuideUtilsTest {
    @Test
    fun `moving a line endpoint recalculates its centre`() {
        val guide = RulerGuide(
            mode = RulerMode.LINE,
            start = Point2(0f, 0f),
            end = Point2(100f, 0f),
            center = Point2(50f, 0f),
        )

        val moved = moveGuide(guide, handle = 0, delta = Point2(20f, 30f))

        assertEquals(20f, moved.start.x, 0.001f)
        assertEquals(30f, moved.start.y, 0.001f)
        assertEquals(60f, moved.center.x, 0.001f)
        assertEquals(15f, moved.center.y, 0.001f)
    }

    @Test
    fun `moving a mirror centre translates both axis endpoints`() {
        val guide = RulerGuide(
            mode = RulerMode.MIRROR,
            start = Point2(0f, 0f),
            end = Point2(100f, 0f),
            center = Point2(50f, 0f),
        )

        val moved = moveGuide(guide, handle = 2, delta = Point2(10f, -20f))

        assertEquals(10f, moved.start.x, 0.001f)
        assertEquals(-20f, moved.start.y, 0.001f)
        assertEquals(110f, moved.end.x, 0.001f)
        assertEquals(-20f, moved.end.y, 0.001f)
        assertEquals(60f, moved.center.x, 0.001f)
        assertEquals(-20f, moved.center.y, 0.001f)
    }
}
