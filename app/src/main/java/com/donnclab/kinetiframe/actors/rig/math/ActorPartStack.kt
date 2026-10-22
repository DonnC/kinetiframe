package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.SceneGraph

/** Preserve legacy FK traversal for equal/default ranks, not map insertion order. */
object ActorPartStack {
    fun orderedIds(graph: SceneGraph): List<String> {
        // Metadata-only traversal: reordering must not allocate Android matrices or bitmaps.
        val children = graph.bones.values.groupBy { it.parentId }
        val pending = ArrayDeque(graph.rootBoneIds.asReversed())
        val visited = linkedSetOf<String>()
        while (pending.isNotEmpty()) {
            val id = pending.removeLast()
            if (id !in graph.bones || !visited.add(id)) continue
            children[id].orEmpty().asReversed().forEach { pending.addLast(it.id) }
        }
        return visited.sortedBy { graph.bones[it]?.drawOrder ?: 0 }
    }

    fun move(graph: SceneGraph, boneId: String, forward: Boolean): SceneGraph {
        val ids = orderedIds(graph).toMutableList()
        val from = ids.indexOf(boneId)
        val to = from + if (forward) 1 else -1
        if (from < 0 || to !in ids.indices) return graph
        ids.add(to, ids.removeAt(from))
        val ranks = ids.withIndex().associate { it.value to it.index }
        return graph.copy(
            bones = graph.bones.mapValues { (id, bone) ->
                bone.withDisplay(
                    drawOrder =
                    ranks[id] ?: bone.drawOrder,
                )
            },
        )
    }
}
