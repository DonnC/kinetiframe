package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.studio.SelectionInteractionMode

/** Hit radii are screen-space dp converted to document units by the viewport scale. */
internal object SelectionHandleHitTest {
    const val PIVOT = 4
    const val MOVE = 10
    const val NONE = -1
    const val PIVOT_RADIUS_DP = 8f
    const val HANDLE_RADIUS_DP = 24f

    fun resolve(
        point: Offset,
        handles: List<Pair<Int, Offset>>,
        pivot: Offset,
        moveCenter: Offset,
        mode: SelectionInteractionMode,
        scale: Float,
    ): Int {
        val safeScale = scale.coerceAtLeast(.001f)
        if (mode == SelectionInteractionMode.MOVE) {
            // A dedicated grab target remains usable even when the artwork is only a few pixels.
            return if ((point - moveCenter).getDistance() <= HANDLE_RADIUS_DP / safeScale) MOVE else NONE
        }
        // The pivot must be touched directly. It must not win just because it is nearby;
        // resize targets retain their larger touch affordance independently.
        if ((point - pivot).getDistance() <= PIVOT_RADIUS_DP / safeScale) return PIVOT
        return handles.map { (id, position) -> id to (point - position).getDistance() }
            .filter { it.second <= HANDLE_RADIUS_DP / safeScale }
            .minByOrNull { it.second }?.first ?: NONE
    }
}
