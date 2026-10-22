package com.donnclab.kinetiframe.actors.render

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.project.model.ActorPlacementReference

/** Small posed texture footprints, not additional decoded bitmaps. Frame/references reject stale hits. */
data class ActorSelectionGeometry(val projectId: String, val frame: Int, val objects: List<Object>) {
    data class Object(val placement: ActorPlacementReference, val quads: List<List<Offset>>)
}
