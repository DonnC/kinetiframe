package com.donnclab.kinetiframe.engine.rigging.math

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph

data class ComputedBone(val id: String, val worldMatrix: Matrix, val startPoint: Offset, val endPoint: Offset)

object ForwardKinematicsSolver {
    fun resolveWorldStates(graph: SceneGraph): Map<String, ComputedBone> =
        RigWorldTransforms.fromRoots(graph).mapValuesTo(LinkedHashMap(graph.bones.size)) { (id, affine) ->
            val world = Matrix().apply {
                setValues(
                    floatArrayOf(
                        affine.a, affine.b, affine.tx,
                        affine.c, affine.d, affine.ty,
                        0f, 0f, 1f,
                    ),
                )
            }
            val length = graph.bones.getValue(id).length
            ComputedBone(
                id,
                world,
                Offset(affine.tx, affine.ty),
                Offset(affine.a * length + affine.tx, affine.c * length + affine.ty),
            )
        }
}
