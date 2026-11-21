package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.artwork.SceneArtwork
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.artwork.ArtworkTimeline

/** Stable addresses preserve legacy part-keyed artwork paths while adding actor-owned variants. */
object ActorArtwork {
    fun bindings(graph: SceneGraph): Map<String, TextureBinding> = SceneArtwork.bindings(graph)

    fun selections(keys: List<ActorArtworkKey>, time: Long): Map<String, String> =
        ArtworkTimeline.selections(keys, time)

    fun apply(graph: SceneGraph, keys: List<ActorArtworkKey>, time: Long): SceneGraph =
        ArtworkTimeline.apply(graph, keys, time)

    fun put(keys: List<ActorArtworkKey>, time: Long, part: String, variant: String): List<ActorArtworkKey> =
        ArtworkTimeline.put(keys, time, part, variant)
}
