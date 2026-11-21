// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RigidRigGeometryTest {
    @Test fun `iterative geometry exactly matches frozen recursive joints and insertion order`() {
        val random = Random(14)
        repeat(30) {
            val nodes = (0 until 60).map { index ->
                BoneNode(
                    "$index",
                    if (index == 0 ||
                        index % 9 == 0
                    ) {
                        null
                    } else {
                        "${random.nextInt(index)}"
                    },
                    "$index",
                    random.nextFloat() * 70f + 1f,
                )
                    .withRest(
                        localTransform = Transform(
                            random.nextFloat() * 100f,
                            random.nextFloat() * -100f,
                            random.nextFloat() * 600f,
                        ),
                    )
            }
            val graph = SceneGraph(bones = nodes.shuffled(random).associateBy { it.id })
            val expected = legacy(graph)
            val actual = RigidRigGeometry.resolve(graph)
            assertEquals(expected.keys.toList(), actual.keys.toList())
            expected.forEach { (id, joint) -> assertEquals(joint, actual.getValue(id)) }
        }
    }

    @Test fun `rigid geometry ignores scale but editing guard rejects it`() {
        val bone = BoneNode("root", name = "Root", length = 10f).withRest(localTransform = Transform(3f, 4f, 90f))
        val graph = SceneGraph(listOf("root"), mapOf("root" to bone))
        val scaled = graph.copy(
            bones = mapOf(
                "root" to bone.withRest(localTransform = bone.localTransform.copy(stretch = Point2(-2f, 3f))),
            ),
        )
        assertTrue(RigidRigGeometry.isRigid(graph))
        assertFalse(RigidRigGeometry.isRigid(scaled))
        assertEquals(RigidRigGeometry.resolve(graph), RigidRigGeometry.resolve(scaled))
    }

    @Test fun `full hierarchy depth is iterative and graph remains unchanged`() {
        val bones = (0 until 512).associate { index ->
            "$index" to BoneNode(
                "$index",
                if (index == 0) null else "${index - 1}",
                "$index",
                1f,
            ).withRest(localTransform = Transform(1f, 2f))
        }
        val graph = SceneGraph(listOf("0"), bones)
        assertEquals(Point2(512f, 1024f), RigidRigGeometry.resolve(graph).getValue("511").start)
        assertEquals(bones, graph.bones)
    }

    @Test fun `cycles have typed failures and absent ancestors keep map lookup failure`() {
        val cyclic = SceneGraph(bones = mapOf("a" to BoneNode("a", "a", "A", 1f)))
        assertEquals(
            RigidGeometryIssue.CYCLIC_SKELETON,
            assertThrows(RigidGeometryException::class.java) {
                RigidRigGeometry.resolve(cyclic)
            }.issue,
        )
        assertThrows(NoSuchElementException::class.java) {
            RigidRigGeometry.resolve(SceneGraph(bones = mapOf("a" to BoneNode("a", "missing", "A", 1f))))
        }
        assertTrue(RigidRigGeometry.resolve(SceneGraph()).isEmpty())
    }

    @Test fun `rotation and angle preserve cardinal coordinates`() {
        val point = RigidRigGeometry.rotate(Point2(10f, 0f), 90f)
        assertEquals(0f, point.x, .001f)
        assertEquals(10f, point.y, .001f)
        assertEquals(90f, RigidRigGeometry.angle(point), .001f)
    }

    // Frozen recursive arithmetic checks that iterative traversal changes depth handling, not joint coordinates.
    private fun legacy(graph: SceneGraph): Map<String, RigidJoint> {
        val result = linkedMapOf<String, RigidJoint>()
        fun rotate(point: Point2, degrees: Float): Point2 {
            val radians = Math.toRadians(degrees.toDouble())
            val c = cos(radians).toFloat()
            val s = sin(radians).toFloat()
            return Point2(point.x * c - point.y * s, point.x * s + point.y * c)
        }
        fun visit(id: String): RigidJoint {
            result[id]?.let { return it }
            val bone = graph.bones.getValue(id)
            val parent = bone.parentId?.let(::visit)
            val angle = (parent?.angle ?: 0f) + bone.localTransform.rotationDegrees
            val start = (parent?.start ?: Point2(0f, 0f)) + rotate(bone.localTransform.translation, parent?.angle ?: 0f)
            return RigidJoint(start, start + rotate(Point2(bone.length, 0f), angle), angle).also { result[id] = it }
        }
        graph.bones.keys.forEach(::visit)
        return result
    }
}
