package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix

/**
 * A stable holder for selection transformation state that allows high-frequency
 * updates without triggering full UI recomposition.
 */
class SelectionTransformState(initialMatrix: Matrix = Matrix(), initialPivot: Offset = Offset.Zero) {
    var matrix by mutableStateOf(initialMatrix)
    var pivotOffset by mutableStateOf(initialPivot)

    // UI Feedback states
    var activeHandle by mutableIntStateOf(-1)
    var isTransforming by mutableStateOf(false)
}
