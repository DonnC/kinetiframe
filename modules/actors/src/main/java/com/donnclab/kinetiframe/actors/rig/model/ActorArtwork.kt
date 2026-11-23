package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.artwork.SceneArtwork
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.figure.FigurePart
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

    /** Whether [variant] can be keyed on [part]: a cutout variant or a drawn figure part's variant. */
    fun hasVariant(graph: SceneGraph, part: String, variant: String): Boolean {
        val bone = graph.bones[part] ?: return false
        return variant.isEmpty() || variant in bone.artworkVariants ||
            graph.figure?.parts?.any { it.boneId == part && variant in it.variants } == true
    }

    /** The variants offered for [part] as (ID, name), the default first; empty when it has none. */
    fun choices(graph: SceneGraph, part: String, defaultName: String): List<Pair<String, String>> {
        val bone = graph.bones[part] ?: return emptyList()
        if (bone.artworkVariants.isNotEmpty()) {
            return listOf("" to defaultName) + bone.artworkVariants.map { (id, variant) -> id to variant.name }
        }
        val drawn = graph.figure?.parts?.firstOrNull { it.boneId == part && it.variants.size > 1 } ?: return emptyList()
        return drawn.variants.keys.sortedBy { it != FigurePart.DEFAULT_VARIANT }.map { id ->
            id to if (id == FigurePart.DEFAULT_VARIANT) defaultName else id.replaceFirstChar(Char::titlecase)
        }
    }
}
