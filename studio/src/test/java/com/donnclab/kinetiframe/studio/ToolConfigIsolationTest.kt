package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.DrawingTool
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolConfigIsolationTest {
    @Test
    fun `construction config is independent from brush config`() {
        val brush = ToolConfig(size = 9f, opacity = 0.4f, brushId = BrushId.PEN)
        val construction = ToolConfig(size = 24f, opacity = 0.9f, brushId = BrushId.PENCIL)
        val state = ToolState(
            activeTool = DrawingTool.BRUSH,
            configs = DrawingTool.values().associateWith { ToolConfig() } + (DrawingTool.BRUSH to brush),
            constructionShape = com.donnclab.kinetiframe.studio.drawing.ConstructionShape.CAPSULE,
            constructionConfig = construction,
        )

        assertEquals(construction, state.activeConfig)
        assertEquals(brush, state.configs[DrawingTool.BRUSH])
        assertEquals(BrushId.PENCIL, state.activeConfig.brushId)
    }
}
