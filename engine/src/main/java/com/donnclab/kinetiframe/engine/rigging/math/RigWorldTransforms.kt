// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

enum class RigTraversalIssue { INVALID_SKIN_HIERARCHY }
class RigTraversalException(val issue: RigTraversalIssue) : IllegalArgumentException(issue.name)

object RigWorldTransforms {
    fun fromRoots(graph: SceneGraph): Map<String, SkinAffine> {
        val result = LinkedHashMap<String, SkinAffine>(graph.bones.size)
        val children = graph.bones.values.filter { it.parentId != null }.groupBy { it.parentId }
        val pending = ArrayDeque<WorkItem>()
        graph.rootBoneIds.asReversed().forEach { id ->
            graph.bones[id]?.let { pending.addLast(WorkItem(it, SkinAffine.Identity)) }
        }
        while (pending.isNotEmpty()) {
            val (bone, parent) = pending.removeLast()
            if (bone.id in result) continue
            val world = parent.times(RigTransformMath.renderLocal(bone.localTransform))
            result[bone.id] = world
            children[bone.id].orEmpty().asReversed().forEach { pending.addLast(WorkItem(it, world)) }
        }
        return result
    }

    fun all(graph: SceneGraph): Map<String, SkinAffine> {
        val result = linkedMapOf<String, SkinAffine>()
        val pending = graph.bones.toMutableMap()
        while (pending.isNotEmpty()) {
            // Binding indices retain map-stable parent-first batches, not rendering's depth-first order.
            val ready = pending.values.filter { it.parentId == null || it.parentId in result }
            if (ready.isEmpty()) throw RigTraversalException(RigTraversalIssue.INVALID_SKIN_HIERARCHY)
            ready.forEach { bone ->
                result[bone.id] =
                    (result[bone.parentId] ?: SkinAffine.Identity).times(RigTransformMath.local(bone.localTransform))
                pending.remove(bone.id)
            }
        }
        return result
    }

    private data class WorkItem(val bone: BoneNode, val parent: SkinAffine)
}
