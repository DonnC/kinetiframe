package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.edit.PoseBend
import com.donnclab.kinetiframe.engine.rigging.edit.RigArtworkEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RigPoseEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RigSetupEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RootMoveScope

object ActorPoseEditor {
    fun moveOrigin(graph: SceneGraph, id: String, target: Point2): SceneGraph =
        withLegacyRigDiagnostics { RigSetupEdits.moveOrigin(graph, id, target) }

    fun moveArtwork(graph: SceneGraph, id: String, delta: Point2): SceneGraph =
        withLegacyRigDiagnostics { RigArtworkEdits.move(graph, id, delta) }

    fun moveRoot(graph: SceneGraph, id: String, target: Point2, allRoots: Boolean = false): SceneGraph =
        withLegacyRigDiagnostics {
            RigSetupEdits.moveRoot(
                graph,
                id,
                target,
                if (allRoots) RootMoveScope.ALL else RootMoveScope.SELECTED,
            )
        }

    fun fit(graph: SceneGraph, id: String, target: Point2): SceneGraph =
        withLegacyRigDiagnostics { RigSetupEdits.fit(graph, id, target) }

    fun pose(graph: SceneGraph, id: String, target: Point2, bendSide: Int? = null): SceneGraph =
        withLegacyRigDiagnostics {
            val bend = when (bendSide) {
                null -> PoseBend.AUTOMATIC
                -1 -> PoseBend.NEGATIVE
                1 -> PoseBend.POSITIVE
                else -> PoseBend.INVALID
            }
            RigPoseEdits.pose(graph, id, target, bend)
        }
}
