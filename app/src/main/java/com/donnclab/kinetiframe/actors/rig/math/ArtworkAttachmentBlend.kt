package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Generic socket articulation: keep the overlap behind a pivot attached to the parent,
 * then smoothly hand motion to the child. Unlike a continuous limb this does not replace
 * either surface. Build only during authoring; playback uses the existing mesh renderer. */
object ArtworkAttachmentBlend {
    fun bind(graph: SceneGraph, part: String, binding: TextureBinding, width: Int, height: Int): ArtworkSkin {
        ActorGraphValidation.validate(graph)
        require(width in 1..2048 && height in 1..2048)
        val owner = graph.bones.getValue(part)
        val parent = requireNotNull(owner.parentId) { "Root artwork has no attachment to blend" }
        require(binding.skin?.attachmentBlend != false) {
            "Continuous artwork already spans this joint"
        }
        val worlds = ArtworkSkinning.worldTransforms(graph)
        val world = worlds.getValue(part)
        val local = ArtworkBindingTransform.sourceLocal(binding)
        val source = world.times(local)
        val columns = (width / 12).coerceIn(4, 32)
        val rows = (height / 12).coerceIn(4, 32)
        // Limit blending to the proximal fifth: distal artwork (e.g. the face) stays rigid.
        // Use the bone axis, not image rows, so rotated/mirrored artwork behaves identically.
        val reach = owner.length * .2f
        val vertices = (0..rows).flatMap { row ->
            (0..columns).map { column ->
                val sx = width.toFloat() * column / columns
                val sy = height.toFloat() * row / rows
                val t = (local.x(sx, sy) / reach).coerceIn(0f, 1f)
                val weight = t * t * (3 - 2 * t)
                SkinVertex(source.x(sx, sy), source.y(sx, sy), listOf(1f - weight, weight))
            }
        }
        return ArtworkSkin(
            SkinSourceSize(width, height),
            SkinGrid(columns, rows),
            SkinMesh(listOf(SkinBone(parent, worlds.getValue(parent)), SkinBone(part, world)), vertices),
            attachmentBlend = true,
        )
    }
}
