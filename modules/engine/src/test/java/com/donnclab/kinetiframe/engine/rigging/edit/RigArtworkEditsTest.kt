// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RigArtworkEditsTest {
    @Test fun `all alternatives shift alignment locally and skin vertices in actor space`() {
        val original = RigEditFixtures.graph
        val skin =
            ArtworkSkin(
                SkinSourceSize(10, 10),
                SkinGrid(1, 1),
                SkinMesh(
                    listOf(SkinBone("upper", SkinAffine.Identity)),
                    List(4) {
                        SkinVertex(1f, 2f, listOf(1f))
                    },
                ),
            )
        val binding = TextureBinding("default", 3f, 4f).copy(skin = skin)
        val bone = original.bones.getValue("upper").withRest(
            localTransform =
            original.bones.getValue("upper").localTransform.copy(rotationDegrees = 45f),
        )
            .withArtwork(
                textureBinding = binding,
                artworkVariants = mapOf("open" to ArtworkVariant("Open", binding.copy(assetId = "open"))),
            )
        val graph = original.copy(bones = original.bones + ("upper" to bone))
        val moved = RigArtworkEdits.move(graph, "upper", Point2(10f, 0f)).bones.getValue("upper")
        listOf(requireNotNull(moved.textureBinding), moved.artworkVariants.getValue("open").binding).forEach {
            assertEquals(7.071068f, it.offsetX, .001f)
            assertEquals(-7.071068f, it.offsetY, .001f)
            assertEquals(SkinVertex(11f, 2f, listOf(1f)), requireNotNull(it.skin).vertices.first())
            assertEquals(skin.bones, requireNotNull(it.skin).bones)
        }
        assertSame(bone.rest, moved.rest)
        assertEquals("open", moved.artworkVariants.getValue("open").binding.assetId)
        assertEquals(0f, binding.offsetX)
    }

    @Test fun `missing part and non finite artwork delta do not publish edits`() {
        val graph = RigEditFixtures.graph
        assertSame(graph, RigArtworkEdits.move(graph, "missing", Point2(10f, 0f)))
        assertSame(graph, RigArtworkEdits.move(graph, "upper", Point2(Float.NaN, 0f)))
    }
}
