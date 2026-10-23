package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.Serializable

/**
 * Represents the tools available for drawing.
 */
@Serializable
enum class DrawingTool {
    BRUSH,
    ERASER,
    COLOR_FILL,
    LASSO,
    LASSO_CUT,
    RIG_SELECT,
    BONE,
    GRID,
    ONION_SKIN,
    TEXT,
    SMUDGE,
    BLUR,
}
