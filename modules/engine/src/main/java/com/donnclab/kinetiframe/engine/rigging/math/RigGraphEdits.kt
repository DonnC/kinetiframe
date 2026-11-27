// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object RigGraphEdits {
    fun rotate(graph: SceneGraph, targetBoneId: String, newRotationDegrees: Float): SceneGraph {
        val target = graph.bones[targetBoneId] ?: return graph
        val updated = target.withRest(localTransform = target.localTransform.copy(rotationDegrees = newRotationDegrees))
        return graph.copy(bones = graph.bones.toMutableMap().apply { put(targetBoneId, updated) })
    }
}
