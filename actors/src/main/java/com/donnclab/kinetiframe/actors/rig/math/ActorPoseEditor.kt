package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.edit.PoseBend
import com.donnclab.kinetiframe.engine.rigging.edit.RigArtworkEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RigPoseEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RigSetupEdits
import com.donnclab.kinetiframe.engine.rigging.edit.RootMoveScope

object ActorPoseEditor {
    fun moveOrigin(graph: SceneGraph, id: String, target: Offset): SceneGraph =
        withLegacyRigDiagnostics { RigSetupEdits.moveOrigin(graph, id, Point2(target.x, target.y)) }

    fun moveArtwork(graph: SceneGraph, id: String, delta: Offset): SceneGraph =
        withLegacyRigDiagnostics { RigArtworkEdits.move(graph, id, Point2(delta.x, delta.y)) }

    fun moveRoot(graph: SceneGraph, id: String, target: Offset, allRoots: Boolean = false): SceneGraph =
        withLegacyRigDiagnostics {
            RigSetupEdits.moveRoot(
                graph,
                id,
                Point2(target.x, target.y),
                if (allRoots) RootMoveScope.ALL else RootMoveScope.SELECTED,
            )
        }

    fun fit(graph: SceneGraph, id: String, target: Offset): SceneGraph =
        withLegacyRigDiagnostics { RigSetupEdits.fit(graph, id, Point2(target.x, target.y)) }

    fun pose(graph: SceneGraph, id: String, target: Offset, bendSide: Int? = null): SceneGraph =
        withLegacyRigDiagnostics {
            val bend = when (bendSide) {
                null -> PoseBend.AUTOMATIC
                -1 -> PoseBend.NEGATIVE
                1 -> PoseBend.POSITIVE
                else -> PoseBend.INVALID
            }
            RigPoseEdits.pose(graph, id, Point2(target.x, target.y), bend)
        }
}
