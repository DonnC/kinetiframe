// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.math.Fabrik
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk
import kotlin.math.abs

object RigPoseEdits {
    /** Pose edits rotations only. The chain root stays anchored and bone lengths remain fixed. */
    fun pose(graph: SceneGraph, id: String, target: Point2, bend: PoseBend = PoseBend.AUTOMATIC): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite() || !RigidRigGeometry.isRigid(graph)) return graph
        // A middle-joint handle is not an endpoint IK target. Rotate the upper segment
        // towards the dragged knee/elbow while retaining the lower segment's world angle.
        // This lets the artist pass smoothly through straight into either bend direction;
        // endpoint IK subsequently follows the newly authored side, not the rest silhouette.
        val lower = graph.bones.values.singleOrNull { it.parentId == id && it.ikChainRootId == id }
        if (bone.ikChainRootId == null && lower != null) {
            val world = RigidRigGeometry.resolve(graph)
            val delta = target - world.getValue(id).start
            if (delta.getDistance() < RigEditLimits.POINT_EPSILON) return graph
            val angle = RigidRigGeometry.angle(delta)
            val parentAngle = world[bone.parentId]?.angle ?: 0f
            return graph.copy(
                bones = graph.bones +
                    (
                        id to bone.withRest(
                            localTransform = bone.localTransform.copy(rotationDegrees = angle - parentAngle),
                        )
                        ) +
                    (
                        lower.id to lower.withRest(
                            localTransform = lower.localTransform.copy(
                                rotationDegrees =
                                world.getValue(lower.id).angle - angle,
                            ),
                        )
                        ),
            )
        }
        // Hands/feet are attached to a two-bone endpoint, not independent spinning handles.
        // Move the wrist/ankle through IK while preserving the child's world orientation.
        val endpoint = graph.bones[bone.parentId]?.takeIf {
            bone.ikChainRootId == null && it.ikChainRootId != null &&
                abs(bone.localTransform.x - it.length) < RigEditLimits.POINT_EPSILON &&
                abs(bone.localTransform.y) < RigEditLimits.POINT_EPSILON
        }
        if (endpoint != null) {
            val before = RigidRigGeometry.resolve(graph)
            val child = before.getValue(id)
            val offset = child.end - before.getValue(endpoint.id).end
            val posed = pose(graph, endpoint.id, target - offset, bend)
            val parentAngle = RigidRigGeometry.resolve(posed).getValue(endpoint.id).angle
            return posed.copy(
                bones =
                posed.bones +
                    (
                        id to
                            posed.bones.getValue(id).withRest(
                                localTransform = bone.localTransform.copy(
                                    rotationDegrees =
                                    child.angle - parentAngle,
                                ),
                            )
                        ),
            )
        }
        val ids = mutableListOf(id)
        val rootId = bone.ikChainRootId ?: id
        while (ids.last() != rootId) {
            val parent = graph.bones[ids.last()]?.parentId ?: return graph
            if (parent in ids) return graph
            ids += parent
        }
        ids.reverse()
        val world = RigidRigGeometry.resolve(graph)
        val points = ids.map { world.getValue(it).start } + world.getValue(id).end
        if (points.zipWithNext().any { (a, b) -> (b - a).getDistance() < RigEditLimits.POINT_EPSILON }) return graph
        val solved = if (points.size == 3) {
            TwoBoneIk.solve(points, target, bend.resolve(points))
        } else {
            Fabrik.solve(points, target)
        }
        val changed = graph.bones.toMutableMap()
        var parentAngle = world[graph.bones.getValue(ids.first()).parentId]?.angle ?: 0f
        ids.forEachIndexed { index, key ->
            val node = changed.getValue(key)
            val angle = RigidRigGeometry.angle(solved[index + 1] - solved[index])
            changed[key] =
                node.withRest(localTransform = node.localTransform.copy(rotationDegrees = angle - parentAngle))
            parentAngle = angle
        }
        return graph.copy(bones = changed)
    }
}
