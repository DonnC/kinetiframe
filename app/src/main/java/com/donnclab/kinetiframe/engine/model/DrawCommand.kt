package com.donnclab.kinetiframe.engine.model

import com.donnclab.kinetiframe.engine.drawing.BrushSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents a single drawing operation (a stroke) that can be undone or redone.
 */
@Serializable
data class DrawCommand(
    val points: List<TouchPoint>,
    val tool: DrawingTool,
    val color: Int,
    val width: Float,
    val alpha: Float = 1.0f,
    val strength: Float = 0.5f,
    val brush: BrushSnapshot = BrushSnapshot(),
    @Transient
    var cachedPath: android.graphics.Path? = null,
)
