// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import kotlin.math.abs

object RigSetupEdits {
    /** Move an attachment origin in its parent's coordinates, not a rotation endpoint. */
    fun moveOrigin(graph: SceneGraph, id: String, target: Point2): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite() || !RigidRigGeometry.isRigid(graph)) return graph
        val parentBone = graph.bones[bone.parentId]
        // An elbow/wrist sharing its parent's endpoint is one joint, not two detachable pins.
        if (parentBone != null && abs(
                bone.localTransform.x - parentBone.length,
            ) < RigEditLimits.ATTACHMENT_FIT_EPSILON &&
            abs(bone.localTransform.y) < RigEditLimits.ATTACHMENT_FIT_EPSILON
        ) {
            return fit(graph, parentBone.id, target)
        }
        val parent = RigidRigGeometry.resolve(graph)[bone.parentId]
        val local = if (parent == null) target else RigidRigGeometry.rotate(target - parent.start, -parent.angle)
        return graph.copy(
            bones =
            graph.bones +
                (
                    id to bone.withRest(
                        localTransform = bone.localTransform.copy(translation = Point2(local.x, local.y)),
                    )
                    ),
        )
    }

    /** Moving the root positions the whole rest skeleton without changing its proportions. */
    fun moveRoot(
        graph: SceneGraph,
        id: String,
        target: Point2,
        scope: RootMoveScope = RootMoveScope.SELECTED,
    ): SceneGraph {
        val root = graph.bones[id]?.takeIf { it.parentId == null } ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite()) return graph
        if (scope == RootMoveScope.ALL) {
            val dx = target.x - root.localTransform.x
            val dy = target.y - root.localTransform.y
            return graph.copy(
                bones = graph.bones.mapValues { (_, bone) ->
                    if (bone.parentId !=
                        null
                    ) {
                        bone
                    } else {
                        bone.withRest(
                            localTransform = bone.localTransform.copy(
                                translation = Point2(bone.localTransform.x + dx, bone.localTransform.y + dy),
                            ),
                        )
                    }
                },
            )
        }
        return graph.copy(
            bones =
            graph.bones +
                (
                    id to
                        root.withRest(
                            localTransform = root.localTransform.copy(translation = Point2(target.x, target.y)),
                        )
                    ),
        )
    }

    /** Setup changes the chosen endpoint and carries attached children along with it. */
    fun fit(graph: SceneGraph, id: String, target: Point2): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!RigidRigGeometry.isRigid(graph)) return graph
        val world = RigidRigGeometry.resolve(graph)
        val delta = target - world.getValue(id).start
        if (delta.getDistance() < RigEditLimits.MIN_FIT_LENGTH) return graph
        val parentAngle = world[bone.parentId]?.angle ?: 0f
        val length = delta.getDistance()
        val changed = graph.bones.mapValues { (key, node) ->
            when {
                key == id -> node.withRest(
                    length = length,
                    localTransform = node.localTransform.copy(
                        rotationDegrees = RigidRigGeometry.angle(delta) - parentAngle,
                    ),
                )

                node.parentId == id && abs(
                    node.localTransform.x - bone.length,
                ) < RigEditLimits.ATTACHMENT_FIT_EPSILON ->
                    node.withRest(
                        localTransform = node.localTransform.copy(translation = Point2(length, node.localTransform.y)),
                    )

                else -> node
            }
        }
        return graph.copy(bones = changed)
    }
}
