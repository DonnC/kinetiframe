package com.donnclab.kinetiframe.actors.domain

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.data.RotationCurve
import com.donnclab.kinetiframe.actors.data.RotationCurvePoint
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.math.TwoBoneSolver
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecordedContactBendsTest {
    @Test fun `recorded opposite knee bend survives planted contact replay and restores outside take`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val before = ActorPoseEditor.pose(
            rest,
            "left_shin",
            RigGeometry.resolve(rest).getValue("left_shin").end + Offset(40f, -30f),
            -1,
        )
        val anchor = ActorMotionPose.captureContact(before, "left_shin", "left_foot")
        val base = listOf(ActorPoseTrack.capture(0, before, rest, mapOf("left_shin" to anchor)))
        val opposite = ActorPoseEditor.pose(before, "left_shin", Offset(anchor.x, anchor.y), 1)
        val curves = listOf("left_thigh", "left_shin").map { id ->
            RotationCurve(
                id,
                points = listOf(
                    RotationCurvePoint(100_000, before.bones.getValue(id).localTransform.rotationDegrees),
                    RotationCurvePoint(200_000, opposite.bones.getValue(id).localTransform.rotationDegrees),
                ),
            )
        }
        val keys = RecordedContactBends.merge(
            base,
            rest,
            curves,
            setOf("left_thigh", "left_shin"),
            listOf(100_000, 200_000),
            100_000,
            200_000,
            400_000,
        )
        assertEquals(-1, ActorPoseTrack.contacts(keys, 0).getValue("left_shin").bendSide)
        assertEquals(1, ActorPoseTrack.contacts(keys, 200_000).getValue("left_shin").bendSide)
        assertEquals(-1, ActorPoseTrack.contacts(keys, 201_000).getValue("left_shin").bendSide)
        val replay = ActorMotionPose.apply(
            rest,
            PerformanceEvaluator.curveRotations(curves, 200_000),
            emptyMap(),
            ActorPoseTrack.contacts(keys, 200_000),
        )
        val world = RigGeometry.resolve(replay)
        assertEquals(anchor.x, world.getValue("left_shin").end.x, .01f)
        assertEquals(anchor.y, world.getValue("left_shin").end.y, .01f)
        assertEquals(
            1,
            TwoBoneSolver.bendSide(
                listOf(
                    world.getValue("left_thigh").start,
                    world.getValue("left_shin").start,
                    world.getValue("left_shin").end,
                ),
            ),
        )
    }
}
