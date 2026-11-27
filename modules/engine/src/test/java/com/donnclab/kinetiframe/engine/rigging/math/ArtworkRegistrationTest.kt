// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkAlignment
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ArtworkRegistrationTest {
    @Test fun `pivot maps to offset after scaling and rotation`() {
        val binding = TextureBinding("source", Point2(20f, 10f), ArtworkAlignment(Point2(7f, 9f), 90f, 2f))
        val matrix = ArtworkRegistration.sourceLocal(binding)
        assertEquals(7f, matrix.x(20f, 10f), .001f)
        assertEquals(9f, matrix.y(20f, 10f), .001f)
        assertEquals(7f, matrix.x(21f, 10f), .001f)
        assertEquals(11f, matrix.y(21f, 10f), .001f)
    }

    @Test fun `rigid edit retains original source and does not adopt replacement skin`() {
        val old = TextureBinding("source", 0f, 0f)
        val updated = old.copy(assetId = "wrong", alignment = ArtworkAlignment(scale = 2f), skin = skin())
        val result = ArtworkRegistration.realign(SkinAffine.Identity, old, updated)
        assertEquals("source", result.assetId)
        assertEquals(updated.alignment, result.alignment)
        assertNull(result.skin)
    }

    @Test fun `skin edit uses actor world basis and preserves bind matrices and weights`() {
        val world = SkinAffine(Point2(0f, -1f), Point2(1f, 0f), Point2(30f, 40f))
        val mesh = skin().let { it.copy(mesh = it.mesh.copy(vertices = listOf(SkinVertex(30f, 44f, listOf(1f))))) }
        val old = TextureBinding("source", 0f, 0f).copy(skin = mesh)
        val updated = old.copy(alignment = ArtworkAlignment(Point2(10f, 0f), 90f, 2f))
        val result = ArtworkRegistration.realign(world, old, updated)
        val vertex = result.skin!!.vertices.single()
        assertEquals(22f, vertex.x, .001f)
        assertEquals(50f, vertex.y, .001f)
        assertEquals(mesh.bones, result.skin!!.bones)
        assertEquals(mesh.vertices.single().weights, vertex.weights)
        assertEquals(44f, old.skin!!.vertices.single().y)
    }

    @Test fun `invalid alignment is typed and scale limits are inclusive`() {
        val binding = TextureBinding("source", 0f, 0f)
        val invalid = listOf(
            binding.copy(pivot = Point2(Float.NaN, 0f)),
            binding.copy(alignment = ArtworkAlignment(scale = 0f)),
            binding.copy(alignment = ArtworkAlignment(rotationDegrees = Float.POSITIVE_INFINITY)),
            binding.copy(alignment = ArtworkAlignment(Point2(0f, Float.NaN))),
            binding.copy(alignment = ArtworkAlignment(scale = 101f)),
        )
        invalid.forEach {
            val failure = assertThrows(ArtworkRegistrationException::class.java) { ArtworkRegistration.sourceLocal(it) }
            assertEquals(ArtworkRegistrationIssue.INVALID_ALIGNMENT, failure.issue)
        }
        listOf(ArtworkRegistration.MIN_SCALE, ArtworkRegistration.MAX_SCALE).forEach {
            assertDoesNotThrow {
                ArtworkRegistration.sourceLocal(binding.copy(alignment = ArtworkAlignment(scale = it)))
            }
        }
    }

    private fun skin() = ArtworkSkin(
        SkinSourceSize(8, 8),
        SkinGrid(1, 1),
        SkinMesh(listOf(SkinBone("part", SkinAffine.Identity)), listOf(SkinVertex(4f, 0f, listOf(1f)))),
    )
}
