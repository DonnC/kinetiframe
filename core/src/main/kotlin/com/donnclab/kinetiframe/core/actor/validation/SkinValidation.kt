// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinTransformException
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import kotlin.math.abs

internal enum class SkinWeightPolicy { NORMALIZED, SPARSE }

internal object SkinValidation {
    fun validGrid(skin: ArtworkSkin): Boolean =
        skin.sourceWidth in GraphLimits.SOURCE_DIMENSION && skin.sourceHeight in GraphLimits.SOURCE_DIMENSION &&
            skin.columns in GraphLimits.GRID_DIMENSION && skin.rows in GraphLimits.GRID_DIMENSION &&
            skin.vertices.size == (skin.columns + 1) * (skin.rows + 1)

    fun requireBind(matrix: SkinAffine, finiteIssue: GraphValidationIssue) {
        requireGraph(
            listOf(matrix.a, matrix.b, matrix.c, matrix.d, matrix.tx, matrix.ty).all(Float::isFinite),
            finiteIssue,
        )
        try {
            matrix.inverse()
        } catch (failure: SkinTransformException) {
            throw GraphValidationException(GraphValidationIssue.SINGULAR_BIND, failure)
        }
    }

    fun validWeights(vertex: SkinVertex, count: Int, policy: SkinWeightPolicy): Boolean =
        vertex.x.isFinite() && vertex.y.isFinite() && vertex.weights.size == count &&
            vertex.weights.all { it.isFinite() && it in GraphLimits.NORMALIZED } &&
            (
                policy == SkinWeightPolicy.NORMALIZED ||
                    vertex.weights.count { it > 0 } in 1..GraphLimits.MAX_CUTOUT_INFLUENCES
                ) &&
            abs(vertex.weights.sum() - 1f) < GraphLimits.WEIGHT_TOLERANCE
}
