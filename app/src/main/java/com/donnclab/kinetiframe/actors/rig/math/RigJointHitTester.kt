package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset

/** Pure nearest-joint selection with an explicit document-space hit radius. */
object RigJointHitTester {
    fun nearestWithin(point: Offset, joints: List<RigJointTarget>, radius: Float): String? {
        val radiusSquared = radius * radius
        return joints.minByOrNull { joint ->
            val dx = joint.position.x - point.x
            val dy = joint.position.y - point.y
            dx * dx + dy * dy
        }?.takeIf { joint ->
            val dx = joint.position.x - point.x
            val dy = joint.position.y - point.y
            dx * dx + dy * dy <= radiusSquared
        }?.id
    }
}

data class RigJointTarget(val id: String, val position: Offset)
