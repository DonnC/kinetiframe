// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ContactBend
import com.donnclab.kinetiframe.core.actor.motion.ContactFollow
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk
import kotlin.math.abs

object MotionContacts {
    /** Keep a planted endpoint, but let an explicitly posed joint choose either bend side. */
    fun followBendSide(graph: SceneGraph, contacts: Map<String, JointContact>): Map<String, JointContact> {
        if (contacts.isEmpty()) return contacts
        val world = RigidRigGeometry.resolve(graph)
        return contacts.mapValues { (id, anchor) ->
            val root = graph.bones[id]?.ikChainRootId
            if (root == null || root !in world || id !in world) {
                anchor
            } else {
                val start = world.getValue(root).start
                val axis = world.getValue(id).end - start
                val middle = world.getValue(id).start - start
                // A straight chain has no handedness. Retain its contact choice rather than
                // inventing a flip from floating-point noise while passing through extension.
                val cross = axis.x * middle.y - axis.y * middle.x
                if (abs(cross) <
                    BEND_EPSILON
                ) {
                    anchor
                } else {
                    anchor.withBendSide(
                        if (cross <
                            0
                        ) {
                            ContactBend.Negative.sign
                        } else {
                            ContactBend.Positive.sign
                        },
                    )
                }
            }
        }
    }

    /** A foot/hand pins its parent's two-bone endpoint and keeps its world orientation. */
    fun contactBone(graph: SceneGraph, selected: String?): String? {
        val bone = graph.bones[selected] ?: return null
        val end = if (bone.ikChainRootId != null) bone else graph.bones[bone.parentId] ?: return null
        val root = graph.bones[end.ikChainRootId] ?: return null
        return end.id.takeIf { end.parentId == root.id }
    }

    fun captureContact(graph: SceneGraph, id: String, selected: String?): JointContact {
        val world = RigidRigGeometry.resolve(graph)
        val bone = graph.bones.getValue(id)
        val root = bone.ikChainRootId ?: throw MotionContactException(MotionContactIssue.MISSING_IK_ROOT)
        val points = listOf(world.getValue(root).start, world.getValue(id).start, world.getValue(id).end)
        val follow = selected?.takeIf { it != id && graph.bones[it]?.parentId == id }
        return JointContact(
            points.last(),
            ContactBend.fromSign(TwoBoneIk.bendSide(points).sign),
            ContactFollow(follow, follow?.let { world.getValue(it).angle } ?: 0f),
        )
    }
    private const val BEND_EPSILON = .001f
}
