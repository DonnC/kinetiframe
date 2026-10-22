package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding
import com.donnclab.kinetiframe.actors.rig.model.Transform
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtworkAttachmentBlendTest {
    private val binding = TextureBinding("head", 20f, 10f)
    private fun graph() = SceneGraph(
        listOf("body"),
        mapOf(
            "body" to BoneNode(
                hierarchy = BoneHierarchy("body", "Body", null),
                rest = BoneRest(100f, localTransform = Transform(40f, 80f)),
            ),
            "head" to BoneNode(
                hierarchy = BoneHierarchy("head", "Head", "body"),
                rest = BoneRest(100f, localTransform = Transform(x = 100f, y = 12f)),
            ),
        ),
    )

    @Test fun `base stays with parent while distal artwork follows head in either direction`() {
        val graph = graph()
        val skin = ArtworkAttachmentBlend.bind(graph, "head", binding, 120, 20)
        for (angle in listOf(-60f, -30f, 0f, 30f, 60f)) {
            val posed = graph.copy(
                bones =
                graph.bones +
                    (
                        "head" to
                            graph.bones.getValue(
                                "head",
                            ).withRest(localTransform = Transform(x = 100f, y = 12f, rotationDegrees = angle))
                        ),
            )
            val worlds = ArtworkSkinning.worldTransforms(posed)
            val points = ArtworkSkinning.deform(skin, worlds)
            skin.vertices.forEachIndexed { i, vertex ->
                val delta = when (vertex.weights) {
                    listOf(1f, 0f) -> worlds.getValue("body").times(skin.bones[0].bindWorld.inverse())
                    listOf(0f, 1f) -> worlds.getValue("head").times(skin.bones[1].bindWorld.inverse())
                    else -> null
                }
                if (delta != null) {
                    assertEquals(delta.x(vertex.x, vertex.y), points[i * 2], .001f)
                    assertEquals(delta.y(vertex.x, vertex.y), points[i * 2 + 1], .001f)
                }
            }
            assertTrue(points.all(Float::isFinite))
        }
        assertEquals(listOf(1f, 0f), skin.vertices.first().weights)
        assertEquals(listOf(0f, 1f), skin.vertices[skin.columns].weights)
        val attached = graph.copy(
            bones =
            graph.bones +
                ("head" to graph.bones.getValue("head").withArtwork(textureBinding = binding.copy(skin = skin))),
        )
        ActorGraphValidation.validate(attached)
        assertEquals(attached, Json.decodeFromString<SceneGraph>(Json.encodeToString(attached)))
    }

    @Test fun `rest pose preserves source and weights remain bounded across rotated registration`() {
        for (rotation in listOf(-90f, 0f, 90f, 180f)) {
            val skin = ArtworkAttachmentBlend.bind(
                graph(),
                "head",
                binding.copy(alignment = binding.alignment.copy(rotationDegrees = rotation)),
                120,
                20,
            )
            val points = ArtworkSkinning.deform(skin, ArtworkSkinning.worldTransforms(graph()))
            skin.vertices.forEachIndexed { i, vertex ->
                assertEquals(vertex.x, points[i * 2], .001f)
                assertEquals(vertex.y, points[i * 2 + 1], .001f)
                assertEquals(1f, vertex.weights.sum(), .0001f)
                assertTrue(vertex.weights.all { it in 0f..1f })
            }
            assertTrue(skin.vertices.size <= 1089)
        }
    }

    @Test fun `root and invalid source dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkAttachmentBlend.bind(graph(), "body", binding, 120, 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkAttachmentBlend.bind(graph(), "head", binding, 4096, 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkAttachmentBlend.bind(
                graph(),
                "head",
                binding.copy(alignment = binding.alignment.copy(scale = Float.NaN)),
                120,
                20,
            )
        }
    }
}
