package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Attachment pins use the same visible endpoint, but preserve their tiny structural length. */
object ActorJointHandle {
    fun isAttachment(graph: SceneGraph, id: String): Boolean =
        graph.bones[id]?.let { it.parentId != null && it.length <= 2f } == true

    fun moveAttachment(graph: SceneGraph, id: String, endpoint: Offset): SceneGraph {
        if (!endpoint.x.isFinite() || !endpoint.y.isFinite()) return graph
        val world = RigGeometry.resolve(graph)[id] ?: return graph
        return ActorPoseEditor.moveOrigin(graph, id, endpoint - (world.end - world.start))
    }
}
