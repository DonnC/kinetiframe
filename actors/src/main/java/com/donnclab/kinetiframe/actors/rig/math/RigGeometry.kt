package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigidGeometryException
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry

object RigGeometry {
    data class Joint(val start: Point2, val end: Point2, val angle: Float)

    fun rotate(point: Point2, degrees: Float): Point2 = RigidRigGeometry.rotate(point, degrees)

    fun angle(vector: Point2): Float = RigidRigGeometry.angle(vector)

    fun resolve(graph: SceneGraph): Map<String, Joint> = try {
        RigidRigGeometry.resolve(graph).mapValues { (_, joint) ->
            Joint(joint.start, joint.end, joint.angle)
        }
    } catch (failure: RigidGeometryException) {
        throw IllegalStateException("Cyclic skeleton")
    }

    fun isRigid(graph: SceneGraph): Boolean = RigidRigGeometry.isRigid(graph)
}
