package com.donnclab.kinetiframe.engine.model

import com.donnclab.kinetiframe.engine.drawing.BrushId
import com.donnclab.kinetiframe.engine.drawing.BrushSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StrokeSubmissionTest {
    @Test fun `queue owns the point snapshot and the original destination and recipe`() {
        val input = mutableListOf(TouchPoint(10f, 10f, timestamp = 1L))
        val command = DrawCommand(
            input,
            DrawingTool.BRUSH,
            0x123456,
            12f,
            0.5f,
            brush = BrushSnapshot(BrushId.PENCIL, seed = 91L),
        )
        val queued = StrokeSubmission.capture(command, 2, "head")
        input.clear()
        val changed = command.copy(tool = DrawingTool.ERASER, color = 0, width = 100f)
        assertEquals(1, queued.command.points.size)
        assertEquals(2, queued.frameIndex)
        assertEquals("head", queued.layerId)
        assertEquals(command.color, queued.command.color)
        assertEquals(12f, queued.command.width)
        assertEquals(0.5f, queued.command.alpha)
        assertEquals(91L, queued.command.brush.seed)
        assertNotEquals(changed.tool, queued.command.tool)
    }

    @Test fun `invalid queue destination is rejected`() {
        val command = DrawCommand(emptyList(), DrawingTool.BRUSH, 0, 1f)
        assertThrows(IllegalArgumentException::class.java) { StrokeSubmission.capture(command, -1, "head") }
        assertThrows(IllegalArgumentException::class.java) { StrokeSubmission.capture(command, 0, "") }
    }
}
