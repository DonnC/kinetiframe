package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.SkinDeformationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class ArtworkSkinningTest {
    @Test fun `missing world retains product diagnostics after kernel extraction`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            ArtworkSkinning.deform(skin(), emptyMap())
        }
        assertEquals("Missing skin bone", failure.message)
        assertTrue(failure.cause is SkinDeformationException)
    }
    private val binding = TextureBinding("arm", 0f, 20f)
    private fun graph() = SceneGraph(
        listOf("upper"),
        mapOf(
            "upper" to BoneNode(
                hierarchy = BoneHierarchy("upper", "Upper", null),
                rest = BoneRest(100f, localTransform = Transform(40f, 80f)),
            ),
            "lower" to BoneNode(
                hierarchy = BoneHierarchy("lower", "Lower", "upper"),
                rest = BoneRest(100f, localTransform = Transform(x = 100f)),
            ),
        ),
    )
    private fun skin() = ArtworkSkinBuilder.bind(graph(), "upper", "lower", binding, 200, 40)

    @Test fun `bind pose reproduces every source vertex exactly`() {
        val skin = skin()
        val actual = ArtworkSkinning.deform(skin, ArtworkSkinning.worldTransforms(graph()))
        skin.vertices.forEachIndexed { i, v ->
            assertEquals(v.x, actual[i * 2], .001f)
            assertEquals(v.y, actual[i * 2 + 1], .001f)
        }
    }

    @Test fun `one rigid delta moves whole skin without shape or volume loss`() {
        val skin = skin()
        val delta = SkinAffine(Point2(0f, -1f), Point2(1f, 0f), Point2(25f, 12f))
        val worlds = skin.bones.associate { it.boneId to delta.times(it.bindWorld) }
        val actual = ArtworkSkinning.deform(skin, worlds)
        skin.vertices.forEachIndexed { i, v ->
            assertEquals(delta.x(v.x, v.y), actual[i * 2], .001f)
            assertEquals(delta.y(v.x, v.y), actual[i * 2 + 1], .001f)
        }
    }
    private fun probe(rotations: List<Float>): FloatArray {
        val matrices = rotations.map { angle ->
            val r = angle * PI / 180
            SkinAffine(
                Point2(cos(r).toFloat(), -sin(r).toFloat()),
                Point2(sin(r).toFloat(), cos(r).toFloat()),
                Point2(0f, 0f),
            )
        }
        val skin = ArtworkSkin(
            SkinSourceSize(1, 1),
            SkinGrid(1, 1),
            SkinMesh(
                matrices.indices.map { SkinBone("b$it", SkinAffine.Identity) },
                List(4) { SkinVertex(10f, 0f, List(matrices.size) { 1f / matrices.size }) },
            ),
        )
        return ArtworkSkinning.deform(skin, matrices.indices.associate { "b$it" to matrices[it] })
    }

    @Test fun `blend preserves radial volume at ninety degree bend`() {
        val point = probe(listOf(0f, 90f))
        assertEquals(10f, hypot(point[0], point[1]), .001f)
        assertEquals(45f, atan2(point[1], point[0]) * 180f / PI.toFloat(), .001f)
    }

    @Test fun `rotation wrap follows short arc instead of flipping`() {
        val point = probe(listOf(179f, -179f))
        assertEquals(-10f, point[0], .001f)
        assertEquals(0f, point[1], .001f)
    }

    @Test fun `scaled bone uses finite affine fallback`() {
        val skin = skin()
        val scale = SkinAffine(Point2(2f, 0f), Point2(0f, 3f), Point2(0f, 0f))
        val actual = ArtworkSkinning.deform(skin, skin.bones.associate { it.boneId to scale.times(it.bindWorld) })
        skin.vertices.forEachIndexed { i, v ->
            assertEquals(v.x * 2, actual[i * 2], .001f)
            assertEquals(v.y * 3, actual[i * 2 + 1], .001f)
        }
    }

    @Test fun `skin bind matrices weights and alignment survive serialization`() {
        val skin = skin()
        val attached = graph().copy(
            bones =
            graph().bones +
                ("upper" to graph().bones.getValue("upper").withArtwork(textureBinding = binding.copy(skin = skin))),
        )
        val restored = Json.decodeFromString<SceneGraph>(Json.encodeToString(attached))
        assertEquals(attached, restored)
        ActorGraphValidation.validate(restored)
        assertNull(Json.decodeFromString<TextureBinding>("""{"assetId":"old","pivotX":0,"pivotY":0}""").skin)
    }

    @Test fun `invalid weights references singular binds and oversized grids are rejected`() {
        val valid = skin()
        fun reject(skin: ArtworkSkin) {
            val attached = graph().copy(
                bones =
                graph().bones +
                    (
                        "upper" to graph().bones.getValue(
                            "upper",
                        ).withArtwork(textureBinding = binding.copy(skin = skin))
                        ),
            )
            assertThrows(IllegalArgumentException::class.java) { ActorGraphValidation.validate(attached) }
        }
        reject(
            valid.copy(
                mesh = valid.mesh.copy(
                    vertices = valid.vertices.map {
                        it.copy(weights = listOf(.2f, .2f))
                    },
                ),
            ),
        )
        reject(valid.copy(mesh = valid.mesh.copy(bones = valid.bones.map { it.copy(boneId = "missing") })))
        reject(
            valid.copy(
                mesh = valid.mesh.copy(
                    bones = valid.bones.map {
                        it.copy(bindWorld = SkinAffine(Point2(0f, 0f), Point2(0f, 0f), Point2(0f, 0f)))
                    },
                ),
            ),
        )
        reject(valid.copy(grid = valid.grid.copy(columns = 33)))
        reject(valid.copy(mesh = valid.mesh.copy(vertices = valid.vertices.map { it.copy(x = Float.NaN) })))
    }

    @Test fun `weights are normalized bounded and distal endpoints stay rigid`() {
        val skin = skin()
        assertTrue(skin.vertices.size <= 1089)
        skin.vertices.forEach { assertEquals(1f, it.weights.sum(), .0001f) }
        assertEquals(listOf(1f, 0f), skin.vertices.first().weights)
        assertEquals(listOf(0f, 1f), skin.vertices[skin.columns].weights)
    }

    @Test fun `sweep through elbow poses is deterministic and continuous`() {
        val skin = skin()
        var previous: FloatArray? = null
        for (angle in 0..120) {
            val posed = graph().copy(
                bones =
                graph().bones +
                    (
                        "lower" to
                            graph().bones.getValue(
                                "lower",
                            ).withRest(localTransform = Transform(x = 100f, rotationDegrees = angle.toFloat()))
                        ),
            )
            val world = ArtworkSkinning.worldTransforms(posed)
            val actual = ArtworkSkinning.deform(skin, world)
            assertArrayEquals(actual, ArtworkSkinning.deform(skin, world))
            assertTrue(actual.all(Float::isFinite))
            previous?.let { last -> assertTrue(actual.indices.all { abs(actual[it] - last[it]) < 3f }) }
            previous = actual
        }
    }
}
