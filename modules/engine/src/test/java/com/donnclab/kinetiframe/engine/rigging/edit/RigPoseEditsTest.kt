// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.BendSide
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RigPoseEditsTest {
    private val graph = RigEditFixtures.graph

    @Test fun `endpoint poses preserve lengths root anchor and selected bend side`() {
        mapOf(
            PoseBend.NEGATIVE to BendSide.NEGATIVE,
            PoseBend.POSITIVE to BendSide.POSITIVE,
        ).forEach { (choice, side) ->
            val target = Point2(80f, 80f)
            val posed = RigPoseEdits.pose(graph, "lower", target, choice)
            val world = RigidRigGeometry.resolve(posed)
            assertTrue((world.getValue("lower").end - target).getDistance() < .001f)
            assertEquals(Point2(0f, 20f), world.getValue("upper").start)
            assertEquals(
                side,
                TwoBoneIk.bendSide(
                    listOf(
                        world.getValue("upper").start,
                        world.getValue("lower").start,
                        world.getValue("lower").end,
                    ),
                ),
            )
            graph.bones.forEach { (id, node) -> assertEquals(node.length, posed.bones.getValue(id).length) }
            assertSame(graph.bones.getValue("other"), posed.bones.getValue("other"))
        }
    }

    @Test fun `middle joint movement retains lower world orientation`() {
        val before = RigidRigGeometry.resolve(graph)
        val posed = RigPoseEdits.pose(graph, "upper", Point2(40f, 100f))
        val after = RigidRigGeometry.resolve(posed)
        assertEquals(before.getValue("lower").angle, after.getValue("lower").angle, .001f)
        assertEquals(80f, posed.bones.getValue("upper").length)
    }

    @Test fun `hand endpoint follows IK without spinning the attached hand`() {
        val before = RigidRigGeometry.resolve(graph).getValue("hand").angle
        val posed = RigPoseEdits.pose(graph, "hand", Point2(90f, 90f), PoseBend.NEGATIVE)
        assertEquals(before, RigidRigGeometry.resolve(posed).getValue("hand").angle, .001f)
        assertEquals(15f, posed.bones.getValue("hand").length)
    }

    @Test fun `automatic bend survives extension and retraction`() {
        listOf(PoseBend.NEGATIVE, PoseBend.POSITIVE).forEach { side ->
            val initial = RigPoseEdits.pose(graph, "lower", Point2(80f, 80f), side)
            val extended = RigPoseEdits.pose(initial, "lower", Point2(1000f, 20f))
            val restored = RigPoseEdits.pose(extended, "lower", Point2(80f, 80f))
            val world = RigidRigGeometry.resolve(restored)
            assertEquals(
                if (side == PoseBend.NEGATIVE) BendSide.NEGATIVE else BendSide.POSITIVE,
                TwoBoneIk.bendSide(
                    listOf(world.getValue("upper").start, world.getValue("lower").start, world.getValue("lower").end),
                ),
            )
        }
    }

    @Test fun `invalid legacy bend is rejected only when an analytic endpoint consumes it`() {
        assertEquals(
            PoseEditIssue.INVALID_BEND,
            assertThrows(PoseEditException::class.java) {
                RigPoseEdits.pose(graph, "lower", Point2(80f, 80f), PoseBend.INVALID)
            }.issue,
        )
        RigPoseEdits.pose(graph, "upper", Point2(40f, 100f), PoseBend.INVALID)
        assertSame(graph, RigPoseEdits.pose(graph, "missing", Point2(80f, 80f), PoseBend.INVALID))
        assertSame(graph, RigPoseEdits.pose(graph, "lower", Point2(Float.NaN, 80f), PoseBend.INVALID))
    }
}
