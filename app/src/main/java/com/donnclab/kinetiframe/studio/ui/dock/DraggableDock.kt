package com.donnclab.kinetiframe.studio.ui.dock

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Screen-space position and drag-bounding for a floating panel that can be dragged anywhere
 * inside its parent but never off the edge -- shared by every such panel in the editor (the
 * drawing tool dock, the pack tray) so this math exists in exactly one place.
 */
@Stable
class DraggableDockState(initialOffset: Offset, private val insetPx: Float) {
    var containerSize by mutableStateOf(IntSize.Zero)
    var surfaceSize by mutableStateOf(IntSize.Zero)
    var offset by mutableStateOf(initialOffset)
        private set

    fun bounded(candidate: Offset): Offset {
        if (containerSize == IntSize.Zero || surfaceSize == IntSize.Zero) return candidate
        val maxX = (containerSize.width - surfaceSize.width - insetPx).coerceAtLeast(insetPx)
        val maxY = (containerSize.height - surfaceSize.height - insetPx).coerceAtLeast(insetPx)
        return Offset(candidate.x.coerceIn(insetPx, maxX), candidate.y.coerceIn(insetPx, maxY))
    }

    /** Jumps straight to [target], clamped -- used to sync from an externally persisted offset. */
    fun snapTo(target: Offset) {
        offset = bounded(target)
    }

    /** Applies one drag-gesture delta, clamped. */
    fun dragBy(delta: Offset) {
        offset = bounded(offset + delta)
    }
}

@Composable
fun rememberDraggableDockState(initialOffset: Offset, insetDp: Dp = 8.dp): DraggableDockState {
    val insetPx = with(LocalDensity.current) { insetDp.toPx() }
    return remember { DraggableDockState(initialOffset, insetPx) }
}

/** Reports this composable's own size to [state] as the space a panel is bounded within -- apply
 *  to the panel's top-level container, not the panel itself. */
fun Modifier.dockContainer(state: DraggableDockState): Modifier = onSizeChanged { state.containerSize = it }

/** Positions this composable at [DraggableDockState.offset] and reports its size back to [state]
 *  -- apply to the floating panel's own surface. */
fun Modifier.dockSurface(state: DraggableDockState): Modifier = this
    .offset { IntOffset(state.offset.x.roundToInt(), state.offset.y.roundToInt()) }
    .onSizeChanged { state.surfaceSize = it }

/** Wires plain drag-to-move onto whatever this is applied to (a grip icon, a header row).
 *  [onDragStart]/[onDragEnd]/[onDragCancel] are optional hooks for a caller that needs to track
 *  its own dragging flag or persist the final position once the gesture settles. */
fun Modifier.dockDragHandle(
    state: DraggableDockState,
    onDragStart: () -> Unit = {},
    onDragEnd: (Offset) -> Unit = {},
    onDragCancel: () -> Unit = {},
): Modifier = pointerInput(state) {
    detectDragGestures(
        onDragStart = { onDragStart() },
        onDragEnd = { onDragEnd(state.offset) },
        onDragCancel = { onDragCancel() },
    ) { change, dragAmount ->
        change.consume()
        state.dragBy(dragAmount)
    }
}
