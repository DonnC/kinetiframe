package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationException
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationIssue
import com.donnclab.kinetiframe.core.actor.validation.SceneGraphValidation

object ActorGraphValidation {
    fun validate(graph: SceneGraph) {
        try {
            SceneGraphValidation.requireValid(graph)
        } catch (failure: GraphValidationException) {
            throw IllegalArgumentException(legacyMessage(failure.issue), failure.cause)
        }
    }

    private fun legacyMessage(issue: GraphValidationIssue): String = when (issue) {
        GraphValidationIssue.PART_COUNT -> "Actor needs 1–512 parts"

        GraphValidationIssue.ROOTS -> "Actor roots do not match its hierarchy"

        GraphValidationIssue.PART_IDENTITY -> "Invalid part identity"

        GraphValidationIssue.VARIANT_COUNT -> "A part supports at most 24 artwork variants"

        GraphValidationIssue.VARIANT_IDENTITY -> "Invalid artwork variant"

        GraphValidationIssue.ARTWORK_ADDRESS -> "Artwork address collides with a part ID"

        GraphValidationIssue.ARTWORK_ALIGNMENT -> "Invalid artwork alignment"

        GraphValidationIssue.ATTACHMENT_PAIR -> "Attachment blend requires its owner and direct parent"

        GraphValidationIssue.SKIN_GRID -> "Invalid skin grid"

        GraphValidationIssue.SKIN_BONES -> "Invalid skin bones"

        GraphValidationIssue.SKIN_BIND_MATRIX -> "Invalid skin bind matrix"

        GraphValidationIssue.SINGULAR_BIND -> "Singular skin bind transform"

        GraphValidationIssue.SKIN_WEIGHTS -> "Invalid skin weights"

        GraphValidationIssue.PART_LENGTH -> "Invalid part length"

        GraphValidationIssue.PART_TRANSFORM -> "Invalid part transform"

        GraphValidationIssue.UNKNOWN_PARENT -> "Unknown parent part"

        GraphValidationIssue.IK_ROOT -> "IK endpoint must reference its direct upper part"

        GraphValidationIssue.HIERARCHY_CYCLE -> "Actor hierarchy contains a cycle"

        GraphValidationIssue.GRID_BUDGET -> "Actor skin grid budget exceeded"

        GraphValidationIssue.DRAWING_CORRECTIONS -> "Invalid drawing binding correction"

        GraphValidationIssue.SURFACE_DESCENDANTS -> "Continuous artwork can only cover its owner's descendants"

        GraphValidationIssue.SURFACE_OVERLAP -> "Continuous artwork surfaces overlap"

        GraphValidationIssue.DRAWING_SOURCES, GraphValidationIssue.DRAWING_ADDRESS,
        GraphValidationIssue.DRAWING_PLACEMENT, GraphValidationIssue.DRAWING_OVERLAYS,
        GraphValidationIssue.DRAWING_UNDERLAYS, GraphValidationIssue.DRAWING_GRID,
        GraphValidationIssue.DRAWING_BONES, GraphValidationIssue.DRAWING_BIND_MATRIX,
        GraphValidationIssue.DRAWING_WEIGHTS,
        -> "Failed requirement."
    }
}
