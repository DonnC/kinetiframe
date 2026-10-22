package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.abs

// Frozen Offset operations guard the full graph result, not just the new kernel's own invariants.
internal object LegacyActorPoseReference {
    /** Move an attachment origin in its parent's coordinates, not a rotation endpoint. */
    fun moveOrigin(graph: SceneGraph, id: String, target: Offset): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite() || !RigGeometry.isRigid(graph)) return graph
        val parentBone = graph.bones[bone.parentId]
        // An elbow/wrist sharing its parent's endpoint is one joint, not two detachable pins.
        if (parentBone != null && abs(bone.localTransform.x - parentBone.length) < .1f &&
            abs(bone.localTransform.y) < .1f
        ) {
            return fit(graph, parentBone.id, target)
        }
        val parent = RigGeometry.resolve(graph)[bone.parentId]
        val local = if (parent == null) target else RigGeometry.rotate(target - parent.start, -parent.angle)
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

    /** Artwork positioning does not change the bone or its semantic expression alternatives. */
    fun moveArtwork(graph: SceneGraph, id: String, delta: Offset): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!delta.x.isFinite() || !delta.y.isFinite()) return graph
        val local = RigGeometry.rotate(delta, -RigGeometry.resolve(graph).getValue(id).angle)
        fun shifted(binding: TextureBinding) = binding.copy(
            alignment = binding.alignment.copy(offset = binding.alignment.offset + Point2(local.x, local.y)),
            skin = binding.skin?.let { skin ->
                skin.copy(
                    mesh = skin.mesh.copy(
                        vertices = skin.vertices.map {
                            it.copy(x = it.x + delta.x, y = it.y + delta.y)
                        },
                    ),
                )
            },
        )
        return graph.copy(
            bones =
            graph.bones +
                (
                    id to
                        bone.withArtwork(
                            textureBinding = bone.textureBinding?.let(::shifted),
                            artworkVariants = bone.artworkVariants.mapValues {
                                it.value.copy(binding = shifted(it.value.binding))
                            },
                        )
                    ),
        )
    }

    /** Moving the root positions the whole rest skeleton without changing its proportions. */
    fun moveRoot(graph: SceneGraph, id: String, target: Offset, allRoots: Boolean = false): SceneGraph {
        val root = graph.bones[id]?.takeIf { it.parentId == null } ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite()) return graph
        if (allRoots) {
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
    fun fit(graph: SceneGraph, id: String, target: Offset): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!RigGeometry.isRigid(graph)) return graph
        val world = RigGeometry.resolve(graph)
        val delta = target - world.getValue(id).start
        if (delta.getDistance() < 1f) return graph
        val parentAngle = world[bone.parentId]?.angle ?: 0f
        val length = delta.getDistance()
        val changed = graph.bones.mapValues { (key, node) ->
            when {
                key == id -> node.withRest(
                    length = length,
                    localTransform = node.localTransform.copy(rotationDegrees = RigGeometry.angle(delta) - parentAngle),
                )

                node.parentId == id && abs(node.localTransform.x - bone.length) < 0.1f ->
                    node.withRest(
                        localTransform = node.localTransform.copy(translation = Point2(length, node.localTransform.y)),
                    )

                else -> node
            }
        }
        return graph.copy(bones = changed)
    }

    /** Pose edits rotations only. The chain root stays anchored and bone lengths remain fixed. */
    fun pose(graph: SceneGraph, id: String, target: Offset, bendSide: Int? = null): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!target.x.isFinite() || !target.y.isFinite() || !RigGeometry.isRigid(graph)) return graph
        // A middle-joint handle is not an endpoint IK target. Rotate the upper segment
        // towards the dragged knee/elbow while retaining the lower segment's world angle.
        // This lets the artist pass smoothly through straight into either bend direction;
        // endpoint IK subsequently follows the newly authored side, not the rest silhouette.
        val lower = graph.bones.values.singleOrNull { it.parentId == id && it.ikChainRootId == id }
        if (bone.ikChainRootId == null && lower != null) {
            val world = RigGeometry.resolve(graph)
            val delta = target - world.getValue(id).start
            if (delta.getDistance() < .001f) return graph
            val angle = RigGeometry.angle(delta)
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
                abs(bone.localTransform.x - it.length) < .001f && abs(bone.localTransform.y) < .001f
        }
        if (endpoint != null) {
            val before = RigGeometry.resolve(graph)
            val child = before.getValue(id)
            val offset = child.end - before.getValue(endpoint.id).end
            val posed = pose(graph, endpoint.id, target - offset, bendSide)
            val parentAngle = RigGeometry.resolve(posed).getValue(endpoint.id).angle
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
        val world = RigGeometry.resolve(graph)
        val points = ids.map { world.getValue(it).start } + world.getValue(id).end
        if (points.zipWithNext().any { (a, b) -> (b - a).getDistance() < 0.001f }) return graph
        val solved = if (points.size ==
            3
        ) {
            LegacyTwoBoneReference.solve(points, target, bendSide ?: LegacyTwoBoneReference.bendSide(points))
        } else {
            FabrikSolver.solve(points, target)
        }
        val changed = graph.bones.toMutableMap()
        var parentAngle = world[graph.bones.getValue(ids.first()).parentId]?.angle ?: 0f
        ids.forEachIndexed { index, key ->
            val node = changed.getValue(key)
            val angle = RigGeometry.angle(solved[index + 1] - solved[index])
            changed[key] =
                node.withRest(localTransform = node.localTransform.copy(rotationDegrees = angle - parentAngle))
            parentAngle = angle
        }
        return graph.copy(bones = changed)
    }
}
