package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.actor.motion.ContactBend
import com.donnclab.kinetiframe.core.actor.motion.ContactFollow
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.model.JointContact
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import kotlin.math.abs

/** One pose pipeline for Studio, project preview, thumbnails and export. Contacts run after root
 * and rotation interpolation. They are authored positions, not accumulated spring/physics state,
 * so a random seek is identical to sequential playback at the same timestamp.
 */
internal object LegacyMotionPoseReference {
    /** Keep a planted endpoint, but let an explicitly posed joint choose either bend side. */
    fun followBendSide(graph: SceneGraph, contacts: Map<String, JointContact>): Map<String, JointContact> {
        if (contacts.isEmpty()) return contacts
        val world = RigGeometry.resolve(graph)
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
                if (abs(cross) < .001f) anchor else anchor.withBendSide(if (cross < 0) -1 else 1)
            }
        }
    }
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
            graph = LegacyActorPoseReference.pose(graph, id, Offset(anchor.x, anchor.y), anchor.bendSide)
            val follow = anchor.followBoneId?.let(graph.bones::get) ?: return@forEach
            val parentAngle = RigGeometry.resolve(graph)[follow.parentId]?.angle ?: 0f
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

    /** A foot/hand pins its parent's two-bone endpoint and keeps its world orientation. */
    fun contactBone(graph: SceneGraph, selected: String?): String? {
        val bone = graph.bones[selected] ?: return null
        val end = if (bone.ikChainRootId != null) bone else graph.bones[bone.parentId] ?: return null
        val root = graph.bones[end.ikChainRootId] ?: return null
        return end.id.takeIf { end.parentId == root.id }
    }

    fun captureContact(graph: SceneGraph, id: String, selected: String?): JointContact {
        val world = RigGeometry.resolve(graph)
        val bone = graph.bones.getValue(id)
        val root = requireNotNull(bone.ikChainRootId)
        val points = listOf(world.getValue(root).start, world.getValue(id).start, world.getValue(id).end)
        val follow = selected?.takeIf { it != id && graph.bones[it]?.parentId == id }
        return JointContact(
            Point2(points.last().x, points.last().y),
            ContactBend.fromSign(TwoBoneSolver.bendSide(points)),
            ContactFollow(follow, follow?.let { world.getValue(it).angle } ?: 0f),
        )
    }
}
