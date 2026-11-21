// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.RigidRigGeometry

object RigArtworkEdits {
    /** Artwork positioning does not change the bone or its semantic expression alternatives. */
    fun move(graph: SceneGraph, id: String, delta: Point2): SceneGraph {
        val bone = graph.bones[id] ?: return graph
        if (!delta.x.isFinite() || !delta.y.isFinite()) return graph
        val local = RigidRigGeometry.rotate(delta, -RigidRigGeometry.resolve(graph).getValue(id).angle)
        fun shifted(binding: TextureBinding) = binding.copy(
            alignment = binding.alignment.copy(offset = binding.alignment.offset + Point2(local.x, local.y)),
            skin = binding.skin?.let { skin ->
                skin.copy(
                    mesh = skin.mesh.copy(
                        vertices = skin.vertices.map {
                            it.copy(x = it.x + delta.x, y = it.y + delta.y)
                        },
                    ),
                )
            },
        )
        return graph.copy(
            bones =
            graph.bones +
                (
                    id to
                        bone.withArtwork(
                            textureBinding = bone.textureBinding?.let(::shifted),
                            artworkVariants = bone.artworkVariants.mapValues {
                                it.value.copy(binding = shifted(it.value.binding))
                            },
                        )
                    ),
        )
    }
}
