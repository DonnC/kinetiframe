package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorRigTest {
    @Test fun `moving root translates the whole rig without altering lengths`() {
        val graph = ActorTemplates.create(ActorTemplate.ANIMAL, StageDimensions.LandscapeHd)
        val world = RigGeometry.resolve(graph)
        val shift = Offset(80f, 40f)
        val moved = ActorPoseEditor.moveRoot(graph, "spine", world.getValue("spine").start + shift)
        val result = RigGeometry.resolve(moved)
        graph.bones.forEach { (id, bone) ->
            assertEquals(bone.length, moved.bones.getValue(id).length)
            assertTrue((result.getValue(id).start - world.getValue(id).start - shift).getDistance() < 0.01f)
        }
    }

    @Test fun `both templates resolve and all IK chains join at their parent endpoints`() {
        ActorTemplate.entries.forEach { template ->
            val graph = ActorTemplates.create(template, StageDimensions.LandscapeHd)
            val world = RigGeometry.resolve(graph)
            assertEquals(graph.bones.size, world.size)
            graph.bones.values.filter { it.ikChainRootId != null }.forEach { bone ->
                assertEquals(bone.parentId, bone.ikChainRootId)
                assertTrue((world.getValue(bone.id).start - world.getValue(bone.parentId!!).end).getDistance() < 0.01f)
            }
        }
    }

    @Test fun `reachable target preserves lengths and root`() {
        val input = listOf(Offset.Zero, Offset(100f, 0f), Offset(150f, 86.60254f))
        val solved = FabrikSolver.solve(input, Offset(75f, 120f))
        assertEquals(input.first(), solved.first())
        assertTrue((solved.last() - Offset(75f, 120f)).getDistance() < 0.1f)
        input.zipWithNext().zip(solved.zipWithNext()).forEach { (original, result) ->
            assertEquals(
                (original.second - original.first).getDistance(),
                (result.second - result.first).getDistance(),
                0.01f,
            )
        }
    }

    @Test fun `unreachable and coincident targets remain finite with fixed lengths`() {
        val input = listOf(Offset.Zero, Offset(100f, 0f), Offset(200f, 0f))
        listOf(Offset(1000f, 0f), Offset.Zero, Offset(150f, 0f)).forEach { target ->
            val result = FabrikSolver.solve(input, target)
            result.forEach { assertTrue(it.x.isFinite() && it.y.isFinite()) }
            result.zipWithNext().forEach { (a, b) -> assertEquals(100f, (b - a).getDistance(), 0.01f) }
        }
    }

    @Test fun `invalid chain is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            FabrikSolver.solve(listOf(Offset.Zero, Offset.Zero), Offset.Zero)
        }
    }

    @Test fun `posing updates only the limb rotations and fitting keeps attached child connected`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val before = RigGeometry.resolve(graph)
        val target = before.getValue("left_forearm").end + Offset(30f, -40f)
        val posed = ActorPoseEditor.pose(graph, "left_forearm", target)
        assertTrue((RigGeometry.resolve(posed).getValue("left_forearm").end - target).getDistance() < 0.1f)
        graph.bones.forEach { (id, bone) ->
            assertEquals(bone.length, posed.bones.getValue(id).length)
            if (id !in listOf("left_forearm", "left_upper_arm")) assertEquals(bone, posed.bones[id])
        }
        val fitted = ActorPoseEditor.fit(
            graph,
            "left_upper_arm",
            before.getValue("left_upper_arm").end + Offset(-50f, 20f),
        )
        val fittedWorld = RigGeometry.resolve(fitted)
        assertTrue(
            (fittedWorld.getValue("left_upper_arm").end - fittedWorld.getValue("left_forearm").start).getDistance() <
                0.1f,
        )
        assertEquals(before, RigGeometry.resolve(graph))
    }

    @Test fun `figure eight IK drags remain finite and preserve every bone length`() {
        var graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val lengths = graph.bones.mapValues { it.value.length }
        val center = RigGeometry.resolve(graph).getValue("left_forearm").end
        repeat(180) { step ->
            val t = (step / 180f) * (2f * Math.PI).toFloat()
            graph =
                ActorPoseEditor.pose(
                    graph,
                    "left_forearm",
                    center + Offset(kotlin.math.sin(t) * 85f, kotlin.math.sin(t * 2f) * 55f),
                )
            RigGeometry.resolve(graph).values.forEach {
                assertTrue(it.start.x.isFinite() && it.start.y.isFinite() && it.end.x.isFinite() && it.end.y.isFinite())
            }
            lengths.forEach { (id, length) -> assertEquals(length, graph.bones.getValue(id).length, 0.001f) }
        }
    }
}
