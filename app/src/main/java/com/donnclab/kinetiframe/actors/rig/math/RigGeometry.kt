package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigidGeometryException
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry

object RigGeometry {
    data class Joint(val start: Offset, val end: Offset, val angle: Float)

    fun rotate(point: Offset, degrees: Float): Offset =
        RigidRigGeometry.rotate(Point2(point.x, point.y), degrees).let { Offset(it.x, it.y) }

    fun angle(vector: Offset): Float = RigidRigGeometry.angle(Point2(vector.x, vector.y))

    fun resolve(graph: SceneGraph): Map<String, Joint> = try {
        RigidRigGeometry.resolve(graph).mapValues { (_, joint) ->
            Joint(Offset(joint.start.x, joint.start.y), Offset(joint.end.x, joint.end.y), joint.angle)
        }
    } catch (failure: RigidGeometryException) {
        throw IllegalStateException("Cyclic skeleton")
    }

    fun isRigid(graph: SceneGraph): Boolean = RigidRigGeometry.isRigid(graph)
}
