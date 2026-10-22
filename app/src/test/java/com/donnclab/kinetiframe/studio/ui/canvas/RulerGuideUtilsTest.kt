package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RulerGuideUtilsTest {
    @Test
    fun `moving a line endpoint recalculates its centre`() {
        val guide = RulerGuide(
            mode = RulerMode.LINE,
            start = Offset(0f, 0f),
            end = Offset(100f, 0f),
            center = Offset(50f, 0f),
        )

        val moved = moveGuide(guide, handle = 0, delta = Offset(20f, 30f))

        assertEquals(20f, moved.start.x, 0.001f)
        assertEquals(30f, moved.start.y, 0.001f)
        assertEquals(60f, moved.center.x, 0.001f)
        assertEquals(15f, moved.center.y, 0.001f)
    }

    @Test
    fun `moving a mirror centre translates both axis endpoints`() {
        val guide = RulerGuide(
            mode = RulerMode.MIRROR,
            start = Offset(0f, 0f),
            end = Offset(100f, 0f),
            center = Offset(50f, 0f),
        )

        val moved = moveGuide(guide, handle = 2, delta = Offset(10f, -20f))

        assertEquals(10f, moved.start.x, 0.001f)
        assertEquals(-20f, moved.start.y, 0.001f)
        assertEquals(110f, moved.end.x, 0.001f)
        assertEquals(-20f, moved.end.y, 0.001f)
        assertEquals(60f, moved.center.x, 0.001f)
        assertEquals(-20f, moved.center.y, 0.001f)
    }
}
