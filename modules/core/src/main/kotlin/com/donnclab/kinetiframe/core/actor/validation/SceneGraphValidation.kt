// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.artwork.SceneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

// Validate before recursive FK or rendering can encounter cycles or non-finite transforms.
object SceneGraphValidation {
    fun requireValid(graph: SceneGraph) {
        requireGraph(
            graph.bones.isNotEmpty() && graph.bones.size <= GraphLimits.MAX_PARTS,
            GraphValidationIssue.PART_COUNT,
        )
        val roots = graph.bones.values.filter { it.parentId == null }.map { it.id }.toSet()
        requireGraph(
            graph.rootBoneIds.isNotEmpty() && graph.rootBoneIds.size == graph.rootBoneIds.distinct().size &&
                graph.rootBoneIds.toSet() == roots,
            GraphValidationIssue.ROOTS,
        )
        val completed = mutableSetOf<String>()
        graph.bones.forEach { (id, bone) ->
            requireGraph(id == bone.id && id.isNotBlank(), GraphValidationIssue.PART_IDENTITY)
            PartArtworkValidation.requireValid(bone, graph)
            requireGraph(bone.length.isFinite() && bone.length > 0f, GraphValidationIssue.PART_LENGTH)
            val t = bone.localTransform
            requireGraph(
                listOf(t.x, t.y, t.rotationDegrees, t.scaleX, t.scaleY).all(Float::isFinite) &&
                    t.scaleX != 0f && t.scaleY != 0f,
                GraphValidationIssue.PART_TRANSFORM,
            )
            requireGraph(bone.parentId == null || bone.parentId in graph.bones, GraphValidationIssue.UNKNOWN_PARENT)
            requireGraph(
                bone.ikChainRootId == null ||
                    (bone.ikChainRootId == bone.parentId && bone.ikChainRootId in graph.bones),
                GraphValidationIssue.IK_ROOT,
            )
            val path = mutableSetOf<String>()
            var current: String? = id
            // Imported depth is bounded by part count, not by the call stack.
            while (current != null && current !in completed) {
                requireGraph(path.add(current), GraphValidationIssue.HIERARCHY_CYCLE)
                current =
                    (
                        graph.bones[current] ?: throw GraphValidationException(
                            GraphValidationIssue.UNKNOWN_PARENT,
                        )
                        ).parentId
            }
            completed.addAll(path)
        }
        requireGraph(
            SceneArtwork.bindings(graph).values.sumOf { it.skin?.vertices?.size ?: 0 } +
                (graph.drawing?.skin?.vertices?.size ?: 0) <= GraphLimits.MAX_VERTICES,
            GraphValidationIssue.GRID_BUDGET,
        )
        graph.drawing?.let { DrawingValidation.requireValid(it, graph) }
        requireSurfaces(graph)
    }

    private fun requireSurfaces(graph: SceneGraph) {
        val surfaceOwners = mutableMapOf<String, String>()
        graph.bones.values.forEach { owner ->
            (listOfNotNull(owner.textureBinding) + owner.artworkVariants.values.map { it.binding }).forEach { binding ->
                binding.skin?.takeUnless { it.attachmentBlend }?.bones?.forEach { influence ->
                    var ancestor: String? = influence.boneId
                    while (ancestor != null && ancestor != owner.id) ancestor = graph.bones.getValue(ancestor).parentId
                    requireGraph(ancestor == owner.id, GraphValidationIssue.SURFACE_DESCENDANTS)
                }
            }
            owner.textureBinding?.skin?.takeUnless { it.attachmentBlend }?.bones?.forEach { influence ->
                requireGraph(
                    surfaceOwners.put(influence.boneId, owner.id) == null,
                    GraphValidationIssue.SURFACE_OVERLAP,
                )
            }
        }
    }
}
