// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.frames

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/** How much a picked-up frame grows, so it reads as lifted off the grid. */
private const val LIFTED_SCALE = 1.06f

/**
 * Drag to reorder in the Frames grid (R3-20): long-press a frame, drag it over another and let go
 * to move it there. The Move earlier and Move later buttons do the same one place at a time.
 */
@Stable
class FrameGridDragState(private val grid: LazyGridState) {
    /** The frame being dragged, or null. */
    var dragging by mutableStateOf<Int?>(null)
        private set

    /** The frame the dragged one is over, where it lands on release. */
    var target by mutableStateOf<Int?>(null)
        private set

    private var offset by mutableStateOf(Offset.Zero)

    fun start(index: Int) {
        dragging = index
        target = index
        offset = Offset.Zero
    }

    fun drag(amount: Offset) {
        val index = dragging ?: return
        offset += amount
        val start = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        val centre = Offset(
            start.offset.x + start.size.width / 2f + offset.x,
            start.offset.y + start.size.height / 2f + offset.y,
        )
        target = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            centre.x >= item.offset.x && centre.x < item.offset.x + item.size.width &&
                centre.y >= item.offset.y && centre.y < item.offset.y + item.size.height
        }?.index ?: target
    }

    /** Ends the drag and returns the move to make, if the frame was dropped somewhere new. */
    fun finish(): Pair<Int, Int>? {
        val move = dragging?.let { from -> target?.takeIf { it != from }?.let { from to it } }
        dragging = null
        target = null
        offset = Offset.Zero
        return move
    }

    /** Follows the finger while [index] is the frame being dragged. */
    fun Modifier.lifted(index: Int): Modifier = if (dragging == index) {
        zIndex(1f).graphicsLayer {
            translationX = offset.x
            translationY = offset.y
            scaleX = LIFTED_SCALE
            scaleY = LIFTED_SCALE
        }
    } else {
        this
    }
}

/** Long-press and drag gestures for the frame at [index]; [onMove] gets (from, to) on release. */
fun Modifier.frameDrag(state: FrameGridDragState, index: Int, onMove: (Int, Int) -> Unit): Modifier =
    pointerInput(state, index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(index) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount)
            },
            onDragEnd = { state.finish()?.let { (from, to) -> onMove(from, to) } },
            onDragCancel = { state.finish() },
        )
    }
