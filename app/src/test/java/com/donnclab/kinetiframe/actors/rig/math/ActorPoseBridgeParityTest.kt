package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ActorPoseBridgeParityTest {
    @Test fun `all graph editing operations match frozen Offset authoring on seeded templates`() {
        val random = Random(111)
        listOf(ActorTemplate.HUMAN, ActorTemplate.ANIMAL).forEach { template ->
            val graph = ActorTemplates.create(template, StageDimensions.LandscapeHd)
            val ids = graph.bones.keys.toList()
            repeat(60) { step ->
                val id = ids[random.nextInt(ids.size)]
                val point = Offset(random.nextFloat() * 900f, random.nextFloat() * 650f)
                val delta = Offset(random.nextFloat() * 80f - 40f, random.nextFloat() * 80f - 40f)
                val side = listOf(null, -1, 1)[step % 3]
                assertEquals(
                    LegacyActorPoseReference.moveOrigin(graph, id, point),
                    ActorPoseEditor.moveOrigin(graph, id, point),
                )
                assertEquals(LegacyActorPoseReference.fit(graph, id, point), ActorPoseEditor.fit(graph, id, point))
                assertEquals(
                    LegacyActorPoseReference.moveArtwork(graph, id, delta),
                    ActorPoseEditor.moveArtwork(graph, id, delta),
                )
                assertEquals(
                    LegacyActorPoseReference.pose(graph, id, point, side),
                    ActorPoseEditor.pose(graph, id, point, side),
                )
                val root = graph.rootBoneIds.first()
                assertEquals(
                    LegacyActorPoseReference.moveRoot(graph, root, point, step % 2 == 0),
                    ActorPoseEditor.moveRoot(graph, root, point, step % 2 == 0),
                )
            }
        }
    }

    @Test fun `invalid bends retain endpoint diagnostics while missing parts remain no ops`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val endpoint = graph.bones.values.first { it.ikChainRootId != null }.id
        val target = RigGeometry.resolve(graph).getValue(endpoint).end + Offset(10f, 10f)
        assertEquals(
            assertThrows(IllegalArgumentException::class.java) {
                LegacyActorPoseReference.pose(graph, endpoint, target, 0)
            }.message,
            assertThrows(IllegalArgumentException::class.java) {
                ActorPoseEditor.pose(graph, endpoint, target, 0)
            }.message,
        )
        assertSame(graph, ActorPoseEditor.pose(graph, "missing", target, 0))
        assertSame(graph, ActorPoseEditor.pose(graph, endpoint, Offset(Float.NaN, 0f), 0))
    }

    @Test fun `scaled authoring guard and existing raw fit behavior remain unchanged`() {
        val graph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        val id = graph.rootBoneIds.first()
        val bone = graph.bones.getValue(id)
        val scaled = graph.copy(
            bones = graph.bones + (
                id to bone.withRest(
                    localTransform = bone.localTransform.copy(stretch = Point2(2f, 1f)),
                )
                ),
        )
        assertSame(scaled, ActorPoseEditor.fit(scaled, id, Offset(500f, 200f)))
        assertSame(scaled, ActorPoseEditor.pose(scaled, id, Offset(500f, 200f)))
        assertEquals(
            LegacyActorPoseReference.fit(graph, id, Offset(Float.NaN, 0f)),
            ActorPoseEditor.fit(graph, id, Offset(Float.NaN, 0f)),
        )
    }
}
