package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.ArtworkSkin
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinAffine
import com.donnclab.kinetiframe.engine.rigging.model.SkinBone
import com.donnclab.kinetiframe.engine.rigging.model.SkinGrid
import com.donnclab.kinetiframe.engine.rigging.model.SkinMesh
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.engine.rigging.model.SkinVertex
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ArtworkBindingTransformTest {
    private val graph =
        SceneGraph(bones = mapOf("part" to BoneNode("part", name = "Part", length = 30f)), rootBoneIds = listOf("part"))

    @Test fun `rigid resizing keeps source asset and never changes rig`() {
        val old = TextureBinding("pixels", 0f, 0f)
        val result = ArtworkBindingTransform.apply(
            graph,
            "part",
            old,
            old.copy(
                assetId = "wrong",
                alignment = old.alignment.copy(
                    scale = 2f,
                    offset = com.donnclab.kinetiframe.core.geometry.Point2(5f, 0f),
                ),
            ),
        )
        assertEquals("pixels", result.assetId)
        assertEquals(2f, result.scale)
        assertEquals(30f, graph.bones.getValue("part").length)
    }

    @Test fun `skin scales and rotates rest vertices while keeping weights and bind bones`() {
        val skin =
            ArtworkSkin(
                SkinSourceSize(8, 8),
                SkinGrid(1, 1),
                SkinMesh(
                    listOf(SkinBone("part", SkinAffine.Identity)),
                    listOf(SkinVertex(4f, 0f, listOf(1f))),
                ),
            )
        val old = TextureBinding("pixels", 0f, 0f).copy(skin = skin)
        val result = ArtworkBindingTransform.apply(
            graph,
            "part",
            old,
            old.copy(
                alignment = old.alignment.copy(
                    scale = 2f,
                    rotationDegrees = 90f,
                    offset = com.donnclab.kinetiframe.core.geometry.Point2(10f, 0f),
                ),
            ),
        )
        val vertex = result.skin!!.vertices.single()
        assertEquals(10f, vertex.x, .001f)
        assertEquals(8f, vertex.y, .001f)
        assertEquals(skin.bones, result.skin!!.bones)
        assertEquals(listOf(1f), vertex.weights)
    }

    @Test fun `nonfinite and zero scale are rejected before document mutation`() {
        val old = TextureBinding("pixels", 0f, 0f)
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkBindingTransform.apply(graph, "part", old, old.copy(alignment = old.alignment.copy(scale = 0f)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkBindingTransform.apply(
                graph,
                "part",
                old,
                old.copy(
                    alignment = old.alignment.copy(
                        offset = com.donnclab.kinetiframe.core.geometry.Point2(Float.NaN, 0f),
                    ),
                ),
            )
        }
    }
}
