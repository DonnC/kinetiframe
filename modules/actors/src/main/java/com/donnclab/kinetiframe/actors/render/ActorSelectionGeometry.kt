package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.geometry.Point2

/** Small posed texture footprints, not additional decoded bitmaps. Frame/references reject stale hits. */
data class ActorSelectionGeometry(val projectId: String, val frame: Int, val objects: List<Object>) {
    data class Object(val placement: ActorPlacementReference, val quads: List<List<Point2>>)
}
