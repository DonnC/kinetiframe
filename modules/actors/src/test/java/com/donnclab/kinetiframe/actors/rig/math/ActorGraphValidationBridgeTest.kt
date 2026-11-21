package com.donnclab.kinetiframe.actors.rig.math

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
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationException
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationIssue
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

    @Test fun `validation reports the first failure as a typed issue`() {
        val cases = mapOf(
            SceneGraph() to GraphValidationIssue.PART_COUNT,
            graph.copy(rootBoneIds = emptyList(), bones = mapOf("root" to root.withRest(length = -1f))) to
                GraphValidationIssue.ROOTS,
            graph.copy(bones = mapOf("root" to root.withRest(length = -1f))) to GraphValidationIssue.PART_LENGTH,
        )
        cases.forEach { (candidate, issue) ->
            assertEquals(
                issue,
                assertThrows(GraphValidationException::class.java) {
                    ActorGraphValidation.validate(candidate)
                }.issue,
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
        val failure = assertThrows(GraphValidationException::class.java) { ActorGraphValidation.validate(candidate) }
        assertEquals(GraphValidationIssue.SINGULAR_BIND, failure.issue)
        assertInstanceOf(SkinTransformException::class.java, failure.cause)
    }

    @Test fun `drawing checks report their typed issue`() {
        val drawing = ActorDrawing(
            DrawingSource("same", "same", SkinSourceSize(10, 10)),
            DrawingPlacement(Point2(0f, 0f), DrawingExtent(10f, 10f)),
            DrawingBinding(skin(SkinAffine.Identity)),
        )
        assertEquals(
            GraphValidationIssue.DRAWING_SOURCES,
            assertThrows(GraphValidationException::class.java) {
                ActorGraphValidation.validate(graph.copy(drawing = drawing))
            }.issue,
        )
    }
}
