// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.artwork

import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object ArtworkTimeline {
    // Preserve authored order, including duplicate timestamps in unvalidated input.
    fun selections(keys: List<ActorArtworkKey>, time: Long): Map<String, String> = buildMap {
        keys.forEach { if (it.timeMicros <= time) putAll(it.selections) }
    }

    fun apply(graph: SceneGraph, keys: List<ActorArtworkKey>, time: Long): SceneGraph {
        val selected = selections(keys, time)
        if (selected.isEmpty()) return graph
        return graph.copy(
            bones = graph.bones.mapValues { (id, bone) ->
                val variant = bone.artworkVariants[selected[id]]
                if (variant == null) bone else bone.withArtwork(textureBinding = variant.binding)
            },
        )
    }

    fun put(keys: List<ActorArtworkKey>, time: Long, part: String, variant: String): List<ActorArtworkKey> {
        val existing = keys.firstOrNull { it.timeMicros == time }?.selections.orEmpty()
        return (
            keys.filterNot { it.timeMicros == time } +
                ActorArtworkKey(time, existing + (part to variant))
            ).sortedBy { it.timeMicros }
    }
}
