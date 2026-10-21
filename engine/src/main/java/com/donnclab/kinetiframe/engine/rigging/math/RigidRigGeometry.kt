// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class RigidJoint(val start: Point2, val end: Point2, val angle: Float)
enum class RigidGeometryIssue { CYCLIC_SKELETON }
class RigidGeometryException(val issue: RigidGeometryIssue) : IllegalStateException(issue.name)

object RigidRigGeometry {
    fun rotate(point: Point2, degrees: Float): Point2 {
        val radians = Math.toRadians(degrees.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        return Point2(point.x * c - point.y * s, point.x * s + point.y * c)
    }

    fun angle(vector: Point2): Float = Math.toDegrees(atan2(vector.y, vector.x).toDouble()).toFloat()

    fun resolve(graph: SceneGraph): Map<String, RigidJoint> {
        val result = linkedMapOf<String, RigidJoint>()
        graph.bones.keys.forEach { id ->
            if (id in result) return@forEach
            val path = mutableListOf<String>()
            val visiting = mutableSetOf<String>()
            var current: String? = id
            // Walk ancestors iteratively; imported depth must not consume the application call stack.
            while (current != null && current !in result) {
                if (!visiting.add(current)) throw RigidGeometryException(RigidGeometryIssue.CYCLIC_SKELETON)
                path += current
                current = graph.bones.getValue(current).parentId
            }
            path.asReversed().forEach { key ->
                val bone = graph.bones.getValue(key)
                val parent = result[bone.parentId]
                val angle = (parent?.angle ?: 0f) + bone.localTransform.rotationDegrees
                val start = (parent?.start ?: ORIGIN) + rotate(bone.localTransform.translation, parent?.angle ?: 0f)
                result[key] = RigidJoint(start, start + rotate(Point2(bone.length, 0f), angle), angle)
            }
        }
        return result
    }

    // Rigid authoring intentionally ignores scale; scaled imports need normalization before editing.
    fun isRigid(graph: SceneGraph): Boolean = graph.bones.values.all {
        it.localTransform.scaleX == 1f && it.localTransform.scaleY == 1f
    }

    private val ORIGIN = Point2(0f, 0f)
}
