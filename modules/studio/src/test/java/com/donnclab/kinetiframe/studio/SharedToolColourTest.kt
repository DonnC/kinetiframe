// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import org.junit.Assert.assertEquals
import org.junit.Test

/** R3-21: one current colour for every tool that paints, so fill uses the colour just picked. */
class SharedToolColourTest {
    private val orange = 0xFFFF8800.toInt()

    @Test fun colourPickedWithThePenIsUsedByFill() {
        val picked = StudioState().reduceToolSetting(StudioIntent.Draw.SetBrushColor(orange))
        val filling = picked.copy(tool = picked.tool.copy(activeTool = DrawingTool.COLOR_FILL))
        assertEquals(orange, filling.tool.activeConfig.color)
    }

    @Test fun colourPickedWhileFillingIsUsedByThePenAndShapes() {
        val filling = StudioState().let { it.copy(tool = it.tool.copy(activeTool = DrawingTool.COLOR_FILL)) }
        val picked = filling.reduceToolSetting(StudioIntent.Draw.SetBrushColor(orange))
        assertEquals(orange, picked.tool.configs.getValue(DrawingTool.BRUSH).color)
        assertEquals(orange, picked.tool.configs.getValue(DrawingTool.TEXT).color)
        assertEquals(orange, picked.tool.constructionConfig.color)
    }

    @Test fun colourPickedForAShapeIsUsedByThePen() {
        val shaping = StudioState().let { it.copy(tool = it.tool.copy(constructionShape = ConstructionShape.CAPSULE)) }
        val picked = shaping.reduceToolSetting(StudioIntent.Draw.SetBrushColor(orange))
        assertEquals(orange, picked.tool.configs.getValue(DrawingTool.BRUSH).color)
    }

    @Test fun sizeStaysPerTool() {
        val sized = StudioState().reduceToolSetting(StudioIntent.Draw.SetBrushSize(30f))
        assertEquals(30f, sized.tool.configs.getValue(DrawingTool.BRUSH).size)
        assertEquals(ToolConfig().size, sized.tool.configs.getValue(DrawingTool.COLOR_FILL).size)
    }
}
