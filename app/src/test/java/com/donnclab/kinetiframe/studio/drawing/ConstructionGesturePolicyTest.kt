package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.DrawingTool
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionGesturePolicyTest {
    @Test fun `only one-drag shapes claim an idle brush gesture`() {
        assertTrue(ConstructionGesturePolicy.canHandle(DrawingTool.BRUSH, ConstructionShape.CAPSULE, false))
        assertFalse(ConstructionGesturePolicy.canHandle(DrawingTool.ERASER, ConstructionShape.CAPSULE, false))
        assertTrue(ConstructionGesturePolicy.canHandle(DrawingTool.BRUSH, ConstructionShape.THREE_POINT_ARC, false))
        assertFalse(ConstructionGesturePolicy.canHandle(DrawingTool.BRUSH, ConstructionShape.TRIANGLE, true))
    }
}
