package com.donnclab.kinetiframe.engine.shape

import com.donnclab.kinetiframe.engine.model.DrawingTool

/** Keeps construction gestures from stealing input from non-brush tools. */
object ConstructionGesturePolicy {
    fun canHandle(activeTool: DrawingTool, shape: ConstructionShape?, previewPending: Boolean): Boolean =
        activeTool == DrawingTool.BRUSH && shape != null && !previewPending
}
