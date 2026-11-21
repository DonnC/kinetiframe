package com.donnclab.kinetiframe.actors.rig.math

import android.graphics.Matrix
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigWorldTransforms

data class ComputedBone(val id: String, val worldMatrix: Matrix, val startPoint: Point2, val endPoint: Point2)

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
                Point2(affine.tx, affine.ty),
                Point2(affine.a * length + affine.tx, affine.c * length + affine.ty),
            )
        }
}
