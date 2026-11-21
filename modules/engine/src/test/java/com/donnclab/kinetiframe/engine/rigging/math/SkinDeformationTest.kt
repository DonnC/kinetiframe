// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class SkinDeformationTest {
    @Test fun `missing world transform exposes a typed reason and part ID`() {
        val failure = assertThrows(SkinDeformationException::class.java) {
            SkinDeformation.deform(fixture(listOf(1f)), emptyMap())
        }
        assertEquals(SkinDeformationIssue.MISSING_WORLD_TRANSFORM, failure.issue)
        assertEquals("b0", failure.boneId)
    }
    private fun fixture(weights: List<Float>) = ArtworkSkin(
        SkinSourceSize(10, 10),
        SkinGrid(1, 1),
        SkinMesh(
            weights.indices.map { SkinBone("b$it", SkinAffine.Identity) },
            List(4) { SkinVertex(10f, 0f, weights) },
        ),
    )

    private fun rotation(angle: Float): SkinAffine {
        val radians = angle * PI / 180
        return SkinAffine(
            Point2(cos(radians).toFloat(), -sin(radians).toFloat()),
            Point2(sin(radians).toFloat(), cos(radians).toFloat()),
            Point2(0f, 0f),
        )
    }

    @Test fun `bind pose and one rigid delta reproduce every vertex`() {
        val skin = fixture(listOf(1f))
        val rest = SkinDeformation.deform(skin, mapOf("b0" to SkinAffine.Identity))
        skin.vertices.forEachIndexed { index, vertex ->
            assertEquals(vertex.x, rest[index * 2], .001f)
            assertEquals(vertex.y, rest[index * 2 + 1], .001f)
        }
        val delta = rotation(90f).copy(origin = Point2(25f, 12f))
        val posed = SkinDeformation.deform(skin, mapOf("b0" to delta))
        skin.vertices.forEachIndexed { index, vertex ->
            assertEquals(delta.x(vertex.x, vertex.y), posed[index * 2], .001f)
            assertEquals(delta.y(vertex.x, vertex.y), posed[index * 2 + 1], .001f)
        }
    }

    @Test fun `quaternion blend retains volume and follows short wrap arc`() {
        val skin = fixture(listOf(.5f, .5f))
        val bent = SkinDeformation.deform(skin, mapOf("b0" to rotation(0f), "b1" to rotation(90f)))
        assertEquals(10f, hypot(bent[0], bent[1]), .001f)
        val wrapped = SkinDeformation.deform(skin, mapOf("b0" to rotation(179f), "b1" to rotation(-179f)))
        assertEquals(-10f, wrapped[0], .001f)
        assertEquals(0f, wrapped[1], .001f)
    }

    @Test fun `scale and reflection use deterministic affine fallback without changing bind data`() {
        val skin = fixture(listOf(1f))
        val original = skin.copy()
        val scaled = SkinAffine(Point2(-2f, 0f), Point2(0f, 3f), Point2(0f, 0f))
        val world = mapOf("b0" to scaled)
        val first = SkinDeformation.deform(skin, world)
        assertArrayEquals(first, SkinDeformation.deform(skin, world))
        assertEquals(-20f, first[0], .001f)
        assertEquals(original, skin)
    }
}
