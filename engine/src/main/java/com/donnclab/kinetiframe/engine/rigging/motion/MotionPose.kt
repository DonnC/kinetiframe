// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ContactBend
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.edit.PoseBend
import com.donnclab.kinetiframe.engine.rigging.edit.RigPoseEdits
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry

object MotionPose {
    fun apply(
        rest: SceneGraph,
        rotations: Map<String, Float>,
        roots: Map<String, RootTranslation>,
        contacts: Map<String, JointContact>,
    ): SceneGraph {
        var graph = rest.copy(
            bones = rest.bones.mapValues { (id, bone) ->
                val root = roots[id].takeIf { bone.parentId == null }
                bone.withRest(
                    localTransform = bone.localTransform.copy(
                        rotationDegrees = rotations[id] ?: bone.localTransform.rotationDegrees,
                        translation = Point2(
                            bone.localTransform.x + (root?.x ?: 0f),
                            bone.localTransform.y + (root?.y ?: 0f),
                        ),
                    ),
                )
            },
        )
        contacts.toSortedMap().forEach { (id, anchor) ->
            graph = RigPoseEdits.pose(
                graph,
                id,
                anchor.position,
                when (anchor.bend) {
                    ContactBend.Negative -> PoseBend.NEGATIVE
                    ContactBend.Positive -> PoseBend.POSITIVE
                    is ContactBend.Unrecognized -> PoseBend.INVALID
                },
            )
            val follow = anchor.followBoneId?.let(graph.bones::get) ?: return@forEach
            val parentAngle = RigidRigGeometry.resolve(graph)[follow.parentId]?.angle ?: 0f
            graph =
                graph.copy(
                    bones =
                    graph.bones + (
                        follow.id to follow.withRest(
                            localTransform = follow.localTransform.copy(
                                rotationDegrees = anchor.followWorldAngle - parentAngle,
                            ),
                        )
                        ),
                )
        }
        return graph
    }
}
