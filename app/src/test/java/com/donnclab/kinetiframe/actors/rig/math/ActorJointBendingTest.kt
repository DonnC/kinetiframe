package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorJointBendingTest {
    @Test fun `knees and elbows can cross straight in either direction without resizing bones`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        for ((upper, lower) in listOf(
            "left_thigh" to "left_shin",
            "right_thigh" to "right_shin",
            "left_upper_arm" to "left_forearm",
            "right_upper_arm" to "right_forearm",
        )) {
            val world = RigGeometry.resolve(rest)
            val origin = world.getValue(upper).start
            val lowerAngle = world.getValue(lower).angle
            val sides = listOf(-40f, 40f).map { swing ->
                val target =
                    origin + RigGeometry.rotate(Offset(rest.bones.getValue(upper).length, 0f), lowerAngle + swing)
                val posed = ActorPoseEditor.pose(rest, upper, target)
                val result = RigGeometry.resolve(posed)
                assertEquals(lowerAngle, result.getValue(lower).angle, .001f)
                rest.bones.forEach { (id, bone) -> assertEquals(bone.length, posed.bones.getValue(id).length) }
                assertTrue((result.getValue(upper).end - result.getValue(lower).start).getDistance() < .001f)
                val side = TwoBoneSolver.bendSide(
                    listOf(origin, result.getValue(lower).start, result.getValue(lower).end),
                )
                val followed = ActorPoseEditor.pose(posed, lower, result.getValue(lower).end + Offset(1f, 1f))
                val after = RigGeometry.resolve(followed)
                assertEquals(
                    side,
                    TwoBoneSolver.bendSide(listOf(origin, after.getValue(lower).start, after.getValue(lower).end)),
                )
                side
            }
            assertEquals(setOf(-1, 1), sides.toSet())
        }
    }

    @Test fun `nonfinite joint input is a no op`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        assertEquals(rest, ActorPoseEditor.pose(rest, "left_thigh", Offset(Float.NaN, 0f)))
    }

    @Test fun `endpoint drags retain either bend after full extension and a separate retraction`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val origin = RigGeometry.resolve(rest).getValue("left_thigh").start
        for (side in listOf(-1, 1)) {
            val initial = ActorPoseEditor.pose(rest, "left_shin", origin + Offset(30f, 120f), side)
            val extended = ActorPoseEditor.pose(initial, "left_shin", origin + Offset(0f, 1000f))
            val retracted = ActorPoseEditor.pose(extended, "left_shin", origin + Offset(30f, 120f))
            val world = RigGeometry.resolve(retracted)
            assertEquals(
                side,
                TwoBoneSolver.bendSide(
                    listOf(origin, world.getValue("left_shin").start, world.getValue("left_shin").end),
                ),
            )
        }
    }
}
