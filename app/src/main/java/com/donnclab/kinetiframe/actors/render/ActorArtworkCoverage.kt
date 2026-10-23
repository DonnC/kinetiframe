package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Missing a single bound limb is a broken render, even when the remaining textures decoded. */
internal object ActorArtworkCoverage {
    fun missingParts(graph: SceneGraph, availableAssetIds: Set<String>): List<String> =
        graph.bones.values.filter { bone ->
            bone.visible && bone.textureBinding?.assetId?.let { it !in availableAssetIds } == true
        }.map { it.id }
}
