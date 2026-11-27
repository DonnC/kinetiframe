package com.donnclab.kinetiframe.presentation.actors.rigging

import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Depth-first bone order matching [ActorPartTree]'s own traversal, so any flat UI (the part
 *  rail's chip strip) reads parts in the same order as the hierarchical tree without duplicating
 *  the tree-walk logic. */
fun SceneGraph.orderedBones(): List<BoneNode> {
    val result = mutableListOf<BoneNode>()
    fun walk(id: String) {
        val bone = bones[id] ?: return
        result += bone
        bones.values.filter { it.parentId == id }.forEach { walk(it.id) }
    }
    rootBoneIds.forEach(::walk)
    return result
}
