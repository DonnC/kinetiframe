// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

internal object PartArtworkValidation {
    fun requireValid(bone: BoneNode, graph: SceneGraph) {
        val id = bone.id
        requireGraph(bone.artworkVariants.size <= GraphLimits.MAX_VARIANTS, GraphValidationIssue.VARIANT_COUNT)
        bone.artworkVariants.forEach { (variant, artwork) ->
            requireGraph(
                variant.matches(GraphLimits.VARIANT_ID) && artwork.name.isNotBlank() &&
                    artwork.name.length <= GraphLimits.MAX_VARIANT_NAME,
                GraphValidationIssue.VARIANT_IDENTITY,
            )
            requireGraph("$id#$variant" !in graph.bones, GraphValidationIssue.ARTWORK_ADDRESS)
        }
        (listOfNotNull(bone.textureBinding) + bone.artworkVariants.values.map { it.binding }).forEach { binding ->
            requireGraph(
                binding.assetId.isNotBlank() &&
                    listOf(
                        binding.pivotX,
                        binding.pivotY,
                        binding.offsetX,
                        binding.offsetY,
                        binding.rotationDegrees,
                        binding.scale,
                    )
                        .all(Float::isFinite) && binding.scale in GraphLimits.ARTWORK_SCALE,
                GraphValidationIssue.ARTWORK_ALIGNMENT,
            )
            binding.skin?.let { skin ->
                if (skin.attachmentBlend) {
                    requireGraph(
                        skin.bones.size == 2 &&
                            skin.bones.map {
                                it.boneId
                            }.toSet() == setOf(id, bone.parentId),
                        GraphValidationIssue.ATTACHMENT_PAIR,
                    )
                }
                requireGraph(SkinValidation.validGrid(skin), GraphValidationIssue.SKIN_GRID)
                requireGraph(
                    skin.bones.size in 1..GraphLimits.MAX_CUTOUT_INFLUENCES &&
                        skin.bones.map { it.boneId }.distinct().size == skin.bones.size &&
                        skin.bones.any { it.boneId == id } && skin.bones.all { it.boneId in graph.bones },
                    GraphValidationIssue.SKIN_BONES,
                )
                skin.bones.forEach { SkinValidation.requireBind(it.bindWorld, GraphValidationIssue.SKIN_BIND_MATRIX) }
                skin.vertices.forEach {
                    requireGraph(
                        SkinValidation.validWeights(it, skin.bones.size, SkinWeightPolicy.NORMALIZED),
                        GraphValidationIssue.SKIN_WEIGHTS,
                    )
                }
            }
        }
    }
}
