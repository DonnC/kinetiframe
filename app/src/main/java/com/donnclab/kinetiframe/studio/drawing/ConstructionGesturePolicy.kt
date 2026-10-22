package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.project.model.DrawingTool

/** Keeps construction gestures from stealing input from non-brush tools. */
object ConstructionGesturePolicy {
    fun canHandle(activeTool: DrawingTool, shape: ConstructionShape?, previewPending: Boolean): Boolean =
        activeTool == DrawingTool.BRUSH && shape != null && !previewPending
}
