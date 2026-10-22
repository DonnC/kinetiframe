package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.actors.rig.model.TextureBinding

/** Rest-structure edits become a new bind, so later motion does not inherit an obsolete rest pose.
 * Preserve the visible deformed surface and its weights; do not replace user artwork with a rigid image. */
object ArtworkSkinRestBinding {
    fun rebase(graph: SceneGraph): SceneGraph {
        val world = ArtworkSkinning.worldTransforms(graph)
        fun binding(value: TextureBinding): TextureBinding {
            val skin = value.skin ?: return value
            val points = ArtworkSkinning.deform(skin, world)
            return value.copy(
                skin = skin.copy(
                    mesh = skin.mesh.copy(
                        bones = skin.bones.map { it.copy(bindWorld = world.getValue(it.boneId)) },
                        vertices = skin.vertices.mapIndexed { i, vertex ->
                            vertex.copy(x = points[i * 2], y = points[i * 2 + 1])
                        },
                    ),
                ),
            )
        }
        val rebased = graph.copy(
            bones = graph.bones.mapValues { (_, bone) ->
                bone.withArtwork(
                    textureBinding = bone.textureBinding?.let(::binding),
                    artworkVariants = bone.artworkVariants.mapValues {
                        it.value.copy(binding = binding(it.value.binding))
                    },
                )
            },
        )
        // In sketch mode joint fitting changes influence, not the original linework.
        return ActorDrawingBinder.rebind(rebased)
    }
}
