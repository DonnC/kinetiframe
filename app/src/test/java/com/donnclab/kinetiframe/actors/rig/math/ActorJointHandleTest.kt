package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding
import com.donnclab.kinetiframe.actors.rig.model.Transform
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorJointHandleTest {
    @Test fun `attachment pins move exactly to touch without stretching or rotating`() {
        for (angle in listOf(-90f, 0f, 35f, 180f)) {
            val graph = SceneGraph(
                listOf("head"),
                mapOf(
                    "head" to BoneNode(
                        hierarchy = BoneHierarchy("head", "Head", null),
                        rest = BoneRest(125f, localTransform = Transform(40f, 80f, angle)),
                    ),
                    "label" to BoneNode(
                        hierarchy = BoneHierarchy("label", "Universal artwork", "head"),
                        rest = BoneRest(1f, localTransform = Transform(60f, 10f, 90f)),
                        artwork = BoneArtwork(textureBinding = TextureBinding("art", 10f, 10f)),
                    ),
                ),
            )
            assertTrue(ActorJointHandle.isAttachment(graph, "label"))
            assertFalse(ActorJointHandle.isAttachment(graph, "head"))
            val moved = ActorJointHandle.moveAttachment(graph, "label", Offset(110f, 130f))
            val world = RigGeometry.resolve(moved).getValue("label")
            assertEquals(110f, world.end.x, .001f)
            assertEquals(130f, world.end.y, .001f)
            assertEquals(1f, moved.bones.getValue("label").length)
            assertEquals(graph.bones.getValue("label").textureBinding, moved.bones.getValue("label").textureBinding)
            assertEquals(graph.bones.getValue("head"), moved.bones.getValue("head"))
        }
    }

    @Test fun `nonfinite attachment input is rejected`() {
        val graph = SceneGraph(listOf("part"), mapOf("part" to BoneNode("part", name = "Part", length = 1f)))
        assertEquals(graph, ActorJointHandle.moveAttachment(graph, "part", Offset(Float.NaN, 0f)))
    }
}
