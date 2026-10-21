// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

internal object DrawingValidation {
    fun requireValid(drawing: ActorDrawing, graph: SceneGraph) {
        requireGraph(
            drawing.inkAssetId.isNotBlank() && drawing.colourAssetId.isNotBlank() &&
                drawing.inkAssetId != drawing.colourAssetId,
            GraphValidationIssue.DRAWING_SOURCES,
        )
        requireGraph(
            ActorDrawing.INK_ADDRESS !in graph.bones && ActorDrawing.COLOUR_ADDRESS !in graph.bones,
            GraphValidationIssue.DRAWING_ADDRESS,
        )
        requireGraph(
            drawing.width in GraphLimits.SOURCE_DIMENSION && drawing.height in GraphLimits.SOURCE_DIMENSION &&
                drawing.actorWidth > 0 && drawing.actorHeight > 0 &&
                listOf(drawing.originX, drawing.originY, drawing.actorWidth, drawing.actorHeight).all(Float::isFinite),
            GraphValidationIssue.DRAWING_PLACEMENT,
        )
        val skin = drawing.skin
        requireGraph(
            drawing.overlayParts.size <= GraphLimits.MAX_OVERLAYS && drawing.overlayParts.all { it in graph.bones },
            GraphValidationIssue.DRAWING_OVERLAYS,
        )
        requireGraph(
            drawing.underlayParts.size <= GraphLimits.MAX_OVERLAYS && drawing.underlayParts.all {
                it in graph.bones
            } &&
                drawing.overlayParts.intersect(drawing.underlayParts).isEmpty(),
            GraphValidationIssue.DRAWING_UNDERLAYS,
        )
        requireGraph(
            drawing.regions.size <= GraphLimits.MAX_REGIONS && drawing.regions.all { region ->
                region.boneId in graph.bones && region.outline.size in GraphLimits.REGION_POINTS &&
                    region.featherPixels.isFinite() && region.featherPixels in GraphLimits.FEATHER &&
                    region.outline.all {
                        it.x.isFinite() && it.y.isFinite() &&
                            it.x in GraphLimits.NORMALIZED && it.y in GraphLimits.NORMALIZED
                    }
            },
            GraphValidationIssue.DRAWING_CORRECTIONS,
        )
        requireGraph(
            skin.sourceWidth == drawing.width && skin.sourceHeight == drawing.height &&
                SkinValidation.validGrid(skin) && !skin.attachmentBlend,
            GraphValidationIssue.DRAWING_GRID,
        )
        requireGraph(
            skin.bones.size in 1..GraphLimits.MAX_DRAWING_INFLUENCES &&
                skin.bones.map { it.boneId }.distinct().size == skin.bones.size &&
                skin.bones.all { it.boneId in graph.bones },
            GraphValidationIssue.DRAWING_BONES,
        )
        skin.bones.forEach { SkinValidation.requireBind(it.bindWorld, GraphValidationIssue.DRAWING_BIND_MATRIX) }
        skin.vertices.forEach {
            requireGraph(
                SkinValidation.validWeights(it, skin.bones.size, SkinWeightPolicy.SPARSE),
                GraphValidationIssue.DRAWING_WEIGHTS,
            )
        }
    }
}
