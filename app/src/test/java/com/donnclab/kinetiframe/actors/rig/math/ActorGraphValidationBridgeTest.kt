package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.actors.rig.model.BoneNode
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.DrawingBinding
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingSource
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinTransformException
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActorGraphValidationBridgeTest {
    private val root = BoneNode("root", name = "Root", length = 1f)
    private val graph = SceneGraph(listOf("root"), mapOf("root" to root))
    private fun skin(matrix: SkinAffine) = ArtworkSkin(
        SkinSourceSize(10, 10),
        SkinGrid(1, 1),
        SkinMesh(listOf(SkinBone("root", matrix)), List(4) { SkinVertex(0f, 0f, listOf(1f)) }),
    )

    @Test fun `native validation preserves old messages and first failure order`() {
        val cases = mapOf(
            SceneGraph() to "Actor needs 1–512 parts",
            graph.copy(rootBoneIds = emptyList(), bones = mapOf("root" to root.withRest(length = -1f))) to
                "Actor roots do not match its hierarchy",
            graph.copy(bones = mapOf("root" to root.withRest(length = -1f))) to "Invalid part length",
        )
        cases.forEach { (candidate, message) ->
            assertEquals(
                message,
                assertThrows(IllegalArgumentException::class.java) {
                    ActorGraphValidation.validate(candidate)
                }.message,
            )
        }
    }

    @Test fun `singular transform keeps original math cause instead of typed wrapper`() {
        val singular = SkinAffine(Point2(0f, 0f), Point2(0f, 0f), Point2(0f, 0f))
        val candidate = graph.copy(
            bones = mapOf(
                "root" to root.withArtwork(textureBinding = TextureBinding("body", 0f, 0f).copy(skin = skin(singular))),
            ),
        )
        val failure = assertThrows(IllegalArgumentException::class.java) { ActorGraphValidation.validate(candidate) }
        assertEquals("Singular skin bind transform", failure.message)
        assertInstanceOf(SkinTransformException::class.java, failure.cause)
    }

    @Test fun `drawing checks without legacy messages retain the default diagnostic`() {
        val drawing = ActorDrawing(
            DrawingSource("same", "same", SkinSourceSize(10, 10)),
            DrawingPlacement(Point2(0f, 0f), DrawingExtent(10f, 10f)),
            DrawingBinding(skin(SkinAffine.Identity)),
        )
        assertEquals(
            "Failed requirement.",
            assertThrows(IllegalArgumentException::class.java) {
                ActorGraphValidation.validate(graph.copy(drawing = drawing))
            }.message,
        )
    }
}
