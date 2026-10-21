package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.ActorGraphValidation
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkSkin
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinBone
import com.donnclab.kinetiframe.engine.rigging.model.SkinGrid
import com.donnclab.kinetiframe.engine.rigging.model.SkinMesh
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.engine.rigging.model.SkinVertex
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** Binds a single, continuous arm/leg image to a connected two-bone chain. Built at attachment,
 * never in a draw callback. Source cut lines are not guessed or erased. */
object ArtworkSkinBuilder {
    fun bind(
        graph: SceneGraph,
        owner: String,
        child: String,
        binding: TextureBinding,
        width: Int,
        height: Int,
        softness: Float = .25f,
    ): ArtworkSkin {
        ActorGraphValidation.validate(graph)
        val upper = graph.bones.getValue(owner)
        val lower = graph.bones.getValue(child)
        require(
            lower.parentId == owner && abs(lower.localTransform.x - upper.length) < .1f &&
                abs(lower.localTransform.y) < .1f,
        ) { "Continuous artwork needs an endpoint-connected pair" }
        require(width in 1..2048 && height in 1..2048 && softness in .05f..1f)
        val world = ArtworkSkinning.worldTransforms(graph)
        val local = ArtworkBindingTransform.sourceLocal(binding)
        val source = world.getValue(owner).times(local)
        val columns = (width / 12).coerceIn(4, 32)
        val rows = (height / 12).coerceIn(4, 32)
        val radius = min(upper.length, lower.length) * softness
        fun distance(x: Float, y: Float, id: String): Float {
            val matrix = world.getValue(id)
            val endX = matrix.x(graph.bones.getValue(id).length, 0f)
            val endY = matrix.y(graph.bones.getValue(id).length, 0f)
            val vx = endX - matrix.tx
            val vy = endY - matrix.ty
            val t = (((x - matrix.tx) * vx + (y - matrix.ty) * vy) / (vx * vx + vy * vy)).coerceIn(0f, 1f)
            return hypot(x - matrix.tx - t * vx, y - matrix.ty - t * vy)
        }
        val vertices = (0..rows).flatMap { row ->
            (0..columns).map { column ->
                val x = source.x(width.toFloat() * column / columns, height.toFloat() * row / rows)
                val y = source.y(width.toFloat() * column / columns, height.toFloat() * row / rows)
                // Compact support keeps far ends rigid; cubic falloff makes the blend continuous.
                val upperDistance = distance(x, y, owner)
                val lowerDistance = distance(x, y, child)
                val t = (.5f + (upperDistance - lowerDistance) / (2 * radius)).coerceIn(0f, 1f)
                val lowerWeight = t * t * (3 - 2 * t)
                SkinVertex(x, y, listOf(1 - lowerWeight, lowerWeight))
            }
        }
        return ArtworkSkin(
            SkinSourceSize(width, height),
            SkinGrid(columns, rows),
            SkinMesh(listOf(SkinBone(owner, world.getValue(owner)), SkinBone(child, world.getValue(child))), vertices),
        )
    }
}
