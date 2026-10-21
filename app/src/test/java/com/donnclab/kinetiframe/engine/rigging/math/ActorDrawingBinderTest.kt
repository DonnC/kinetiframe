package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtwork
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawing
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawingRegion
import com.donnclab.kinetiframe.engine.rigging.model.ActorGraphValidation
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.DrawingBinding
import com.donnclab.kinetiframe.engine.rigging.model.DrawingExtent
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPlacement
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPoint
import com.donnclab.kinetiframe.engine.rigging.model.DrawingSource
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorDrawingBinderTest {
    private fun graph() = SceneGraph(
        listOf("body"),
        mapOf(
            "body" to BoneNode("body", name = "Body", length = 60f),
            "arm" to BoneNode("arm", "body", "Arm", 40f).withRest(localTransform = Transform(x = 60f)),
            "eyes" to BoneNode("eyes", "body", "Eyes", 1f).withRest(localTransform = Transform(x = 30f)),
        ),
    )
    private fun document() = ActorDrawing(
        DrawingSource("ink", "colour", SkinSourceSize(100, 40)),
        DrawingPlacement(Point2(0f, -20f), DrawingExtent(100f, 40f)),
        DrawingBinding(
            ActorDrawingBinder.bind(
                graph(),
                SkinSourceSize(100, 40),
                DrawingPlacement(Point2(0f, -20f), DrawingExtent(100f, 40f)),
            ),
        ),
    )

    @Test fun `neutral sketch is unchanged and influences stay normalized and sparse`() {
        val skin = document().skin
        assertFalse(skin.bones.any { it.boneId == "eyes" })
        val vertices = ArtworkSkinning.deform(skin, ArtworkSkinning.worldTransforms(graph()))
        skin.vertices.forEachIndexed { i, vertex ->
            assertEquals(vertex.x, vertices[2 * i], .001f)
            assertEquals(vertex.y, vertices[2 * i + 1], .001f)
            assertEquals(1f, vertex.weights.sum(), .0001f)
            assertTrue(vertex.weights.count { it > 0f } in 1..4)
        }
    }

    @Test fun `sketch and both source addresses survive serialization and mode switch`() {
        val drawn = graph().copy(drawing = document())
        val copy = Json.decodeFromString<SceneGraph>(Json.encodeToString(drawn))
        ActorGraphValidation.validate(copy)
        assertEquals(drawn, copy)
        assertEquals(setOf(ActorDrawing.INK_ADDRESS, ActorDrawing.COLOUR_ADDRESS), ActorArtwork.bindings(copy).keys)
        assertEquals(copy.drawing!!.skin, copy.copy(drawing = copy.drawing!!.withEnabled(false)).drawing!!.skin)
        assertNull(
            Json.decodeFromString<SceneGraph>(
                """{"rootBoneIds":["root"],"bones":{"root":{"id":"root","name":"Root","length":10}}}""",
            ).drawing,
        )
    }

    @Test fun `joint fitting rebinds rather than baking the old deformation into source`() {
        val initial = graph().copy(drawing = document())
        val changed = initial.copy(
            bones =
            initial.bones +
                (
                    "arm" to initial.bones.getValue(
                        "arm",
                    ).withRest(localTransform = Transform(60f, rotationDegrees = 45f))
                    ),
        )
        val rebound = ActorDrawingBinder.rebind(changed)
        val vertices = ArtworkSkinning.deform(rebound.drawing!!.skin, ArtworkSkinning.worldTransforms(rebound))
        rebound.drawing!!.skin.vertices.forEachIndexed { i, v ->
            assertEquals(v.x, vertices[i * 2], .001f)
            assertEquals(
                v.y,
                vertices[
                    i *
                        2 +
                        1,
                ],
                .001f,
            )
        }
        assertEquals("ink", rebound.drawing!!.inkAssetId)
    }

    @Test fun `posed arm moves sketch vertices without NaNs`() {
        val skin = document().skin
        val posed = graph().copy(
            bones =
            graph().bones +
                (
                    "arm" to graph().bones.getValue(
                        "arm",
                    ).withRest(localTransform = Transform(60f, rotationDegrees = 90f))
                    ),
        )
        val vertices = ArtworkSkinning.deform(skin, ArtworkSkinning.worldTransforms(posed))
        assertTrue(vertices.all(Float::isFinite))
        assertTrue(kotlin.math.abs(vertices[skin.columns * 2 + 1] - skin.vertices[skin.columns].y) > 10f)
    }

    @Test fun `invalid source references and weights are rejected`() {
        val doc = document()
        assertThrows(IllegalArgumentException::class.java) {
            ActorGraphValidation.validate(
                graph().copy(drawing = doc.copy(source = doc.source.copy(colourAssetId = "ink"))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActorGraphValidation.validate(
                graph().copy(
                    drawing = doc.withSkin(
                        doc.skin.copy(
                            mesh = doc.skin.mesh.copy(
                                vertices = doc.skin.vertices.map {
                                    it.copy(weights = listOf(0f, 0f))
                                },
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test fun `lasso correction can rigidly bind face art to a tiny facial anchor`() {
        val region =
            ActorDrawingRegion(
                "eyes",
                listOf(DrawingPoint(.1f, .1f), DrawingPoint(.9f, .1f), DrawingPoint(.9f, .9f), DrawingPoint(.1f, .9f)),
                2f,
            )
        val skin = ActorDrawingBinder.bind(
            graph(),
            SkinSourceSize(100, 40),
            DrawingPlacement(Point2(0f, -20f), DrawingExtent(100f, 40f)),
            listOf(region),
        )
        val centre = skin.vertices[16 * 33 + 16]
        assertEquals(1f, centre.weights[skin.bones.indexOfFirst { it.boneId == "eyes" }], .0001f)
        skin.vertices.forEach {
            assertEquals(1f, it.weights.sum(), .0001f)
            assertTrue(it.weights.count { w -> w > 0f } <= 4)
        }
        val doc = document().copy(binding = DrawingBinding(skin, listOf(region)))
        val restored = Json.decodeFromString<SceneGraph>(Json.encodeToString(graph().copy(drawing = doc)))
        ActorGraphValidation.validate(restored)
        assertEquals(listOf(region), restored.drawing!!.regions)
        assertEquals(listOf(region), ActorDrawingBinder.rebind(restored).drawing!!.regions)
    }
}
