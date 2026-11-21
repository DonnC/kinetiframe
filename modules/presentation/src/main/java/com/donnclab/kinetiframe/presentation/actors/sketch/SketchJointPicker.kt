package com.donnclab.kinetiframe.presentation.actors.sketch

import androidx.compose.ui.geometry.Offset

internal data class SketchJointHandle(val partId: String, val start: Boolean, val position: Offset)

internal object SketchJointPicker {
    fun handles(id: String, start: Offset, end: Offset, originOnly: Boolean): List<SketchJointHandle> =
        if (originOnly) {
            listOf(SketchJointHandle(id, true, start))
        } else {
            listOf(SketchJointHandle(id, true, start), SketchJointHandle(id, false, end))
        }

    /** Prefer the selected part only for genuinely overlapping pins, never a farther body pin
     * just because it is selected. Otherwise an eye drag could unexpectedly resize the torso. */
    fun pick(
        handles: List<SketchJointHandle>,
        point: Offset,
        radius: Float,
        tieTolerance: Float,
        selected: String?,
    ): SketchJointHandle? {
        val hits = handles.map { it to (it.position - point).getDistance() }.filter { it.second <= radius }
        val nearest = hits.minByOrNull { it.second } ?: return null
        return hits.filter { it.first.partId == selected && it.second <= nearest.second + tieTolerance }
            .minByOrNull { it.second }?.first ?: nearest.first
    }
}
