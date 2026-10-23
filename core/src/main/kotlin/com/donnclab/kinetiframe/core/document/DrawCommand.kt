package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.Serializable

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
)
