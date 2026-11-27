package com.donnclab.kinetiframe.presentation.studio.canvas

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.studio.drawing.ConstructionShapeSession
import com.donnclab.kinetiframe.studio.drawing.RulerStrokeConstraintSession

/**
 * Bookkeeping for the gesture [CanvasInputLayer] is handling. Only the pointer handler reads and
 * writes these fields, never composition, so they are plain properties rather than Compose state.
 */
internal class CanvasGestureState {
    var lastTapTime = 0L
    var lastTapPoint = Offset.Zero

    /** Consumes the ACTION_UP after a direct node pick so it cannot begin a lasso. */
    var consumedTextSelection = false
    val activePointerSession = ActivePointerSession()
    var currentToolType = MotionEvent.TOOL_TYPE_UNKNOWN
    var rulerSession: RulerStrokeConstraintSession? = null
    var constructionSession: ConstructionShapeSession? = null
    var rigGestureCancelled = false
    var draggedBone: String? = null
    var rigDownScreen = Offset.Zero
}
