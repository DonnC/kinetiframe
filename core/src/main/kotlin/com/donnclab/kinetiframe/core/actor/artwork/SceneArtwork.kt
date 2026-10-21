// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object SceneArtwork {
    fun bindings(graph: SceneGraph): Map<String, TextureBinding> = buildMap {
        graph.bones.forEach { (id, bone) ->
            bone.textureBinding?.let { put(id, it) }
            bone.artworkVariants.forEach { (variant, artwork) -> put("$id#$variant", artwork.binding) }
        }
        graph.drawing?.let { drawing ->
            put(ActorDrawing.INK_ADDRESS, TextureBinding(drawing.inkAssetId, 0f, 0f))
            put(ActorDrawing.COLOUR_ADDRESS, TextureBinding(drawing.colourAssetId, 0f, 0f))
        }
    }
}
