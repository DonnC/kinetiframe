// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RigWorldTransformsTest {
    private fun bone(id: String, parent: String? = null, transform: Transform = Transform()) =
        BoneNode(id, parent, id, 10f).withRest(localTransform = transform)

    @Test fun `render order follows declared roots and child insertion depth first`() {
        val graph = SceneGraph(
            listOf("b", "a"),
            linkedMapOf(
                "a2" to bone("a2", "a"),
                "a21" to bone("a21", "a2"),
                "a" to bone("a"),
                "a1" to bone("a1", "a"),
                "b" to bone("b"),
            ),
        )
        assertEquals(listOf("b", "a", "a2", "a21", "a1"), RigWorldTransforms.fromRoots(graph).keys.toList())
        assertEquals(listOf("a", "b", "a2", "a1", "a21"), RigWorldTransforms.all(graph).keys.toList())
    }

    @Test fun `render skips absent roots duplicates and disconnected parts while skin visits every part`() {
        val graph = SceneGraph(listOf("missing", "a", "a"), linkedMapOf("a" to bone("a"), "b" to bone("b")))
        assertEquals(listOf("a"), RigWorldTransforms.fromRoots(graph).keys.toList())
        assertEquals(listOf("a", "b"), RigWorldTransforms.all(graph).keys.toList())
        assertTrue(RigWorldTransforms.fromRoots(SceneGraph()).isEmpty())
        assertTrue(RigWorldTransforms.all(SceneGraph()).isEmpty())
    }

    @Test fun `skin rejects unresolved parents and cycles with a typed error`() {
        val unknown = SceneGraph(bones = mapOf("a" to bone("a", "missing")))
        val cycle = SceneGraph(listOf("a"), mapOf("a" to bone("a", "b"), "b" to bone("b", "a")))
        listOf(unknown, cycle).forEach { graph ->
            assertEquals(
                RigTraversalIssue.INVALID_SKIN_HIERARCHY,
                assertThrows(RigTraversalException::class.java) { RigWorldTransforms.all(graph) }.issue,
            )
        }
        assertEquals(listOf("a", "b"), RigWorldTransforms.fromRoots(cycle).keys.toList())
    }

    @Test fun `deep validated hierarchy uses no recursive traversal`() {
        val ids = (0 until 512).map(Int::toString)
        val graph = SceneGraph(
            listOf("0"),
            ids.withIndex().associate { (index, id) ->
                id to bone(id, ids.getOrNull(index - 1), Transform(10f, 2f))
            },
        )
        listOf(RigWorldTransforms.fromRoots(graph), RigWorldTransforms.all(graph)).forEach { world ->
            assertEquals(512, world.size)
            assertEquals(5120f, world.getValue("511").tx)
            assertEquals(1024f, world.getValue("511").ty)
        }
    }

    @Test fun `reflected non uniform parent basis transforms child translation and endpoint`() {
        val graph = SceneGraph(
            listOf("root"),
            mapOf(
                "root" to bone("root", transform = Transform(Point2(10f, 20f), 90f, Point2(-2f, 3f))),
                "child" to bone("child", "root", Transform(5f, 1f, 30f)),
            ),
        )
        val child = RigWorldTransforms.fromRoots(graph).getValue("child")
        assertEquals(7f, child.tx, .001f)
        assertEquals(10f, child.ty, .001f)
        assertEquals(child.tx + child.a * 10f, child.x(10f, 0f), .001f)
        assertEquals(child.ty + child.c * 10f, child.y(10f, 0f), .001f)
    }

    @Test fun `render coefficients exactly match frozen legacy arithmetic over seeded forests`() {
        val random = Random(91)
        repeat(20) {
            val nodes = (0 until 40).map { index ->
                bone(
                    "$index",
                    if (index == 0 || index % 7 == 0) null else "${random.nextInt(index)}",
                    Transform(
                        Point2(random.nextFloat() * 100f, random.nextFloat() * -100f),
                        random.nextFloat() * 720f - 360f,
                        Point2(random.nextFloat() * -3f, random.nextFloat() * 4f),
                    ),
                )
            }
            val graph = SceneGraph(nodes.filter { it.parentId == null }.map { it.id }, nodes.associateBy { it.id })
            val actual = RigWorldTransforms.fromRoots(graph)
            val expected = legacyMatrices(graph)
            assertEquals(expected.keys.toList(), actual.keys.toList())
            expected.forEach { (id, values) ->
                val matrix = actual.getValue(id)
                val coefficients = listOf(matrix.a, matrix.b, matrix.c, matrix.d, matrix.tx, matrix.ty)
                values.forEachIndexed { index, value ->
                    assertEquals(value.toRawBits(), coefficients[index].toRawBits(), "$id/$index")
                }
            }
        }
    }

    @Test fun `skin coefficients retain double product rounding`() {
        val random = Random(72)
        repeat(200) {
            val transform = Transform(
                Point2(3f, 4f),
                random.nextFloat() * 720f,
                Point2(random.nextFloat() * -3f, random.nextFloat() * 4f),
            )
            val matrix = RigWorldTransforms.all(
                SceneGraph(listOf("a"), mapOf("a" to bone("a", transform = transform))),
            ).getValue("a")
            val angle = transform.rotationDegrees * PI / 180.0
            val expected = listOf(
                (cos(angle) * transform.scaleX).toFloat(),
                (-sin(angle) * transform.scaleY).toFloat(),
                (sin(angle) * transform.scaleX).toFloat(),
                (cos(angle) * transform.scaleY).toFloat(),
            )
            listOf(matrix.a, matrix.b, matrix.c, matrix.d).forEachIndexed { index, value ->
                assertEquals(expected[index].toRawBits(), value.toRawBits())
            }
        }
    }

    // Independent frozen coefficients protect legacy rounding and parent multiplication, not just geometric tolerance.
    private fun legacyMatrices(graph: SceneGraph): Map<String, FloatArray> {
        val result = linkedMapOf<String, FloatArray>()
        fun visit(id: String, parent: FloatArray) {
            val node = graph.bones.getValue(id)
            val angle = node.localTransform.rotationDegrees * (PI / 180.0)
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            val t = node.localTransform
            val local = floatArrayOf(c * t.scaleX, -s * t.scaleY, s * t.scaleX, c * t.scaleY, t.x, t.y)
            val world = floatArrayOf(
                parent[0] * local[0] + parent[1] * local[2],
                parent[0] * local[1] + parent[1] * local[3],
                parent[2] * local[0] + parent[3] * local[2],
                parent[2] * local[1] + parent[3] * local[3],
                parent[0] * local[4] + parent[1] * local[5] + parent[4],
                parent[2] * local[4] + parent[3] * local[5] + parent[5],
            )
            result[id] = world
            graph.bones.values.filter { it.parentId == id }.forEach { visit(it.id, world) }
        }
        graph.rootBoneIds.forEach { visit(it, floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)) }
        return result
    }
}
