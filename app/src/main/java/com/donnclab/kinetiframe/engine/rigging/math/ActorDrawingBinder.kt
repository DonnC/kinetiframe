package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawingRegion
import com.donnclab.kinetiframe.engine.rigging.model.ActorGraphValidation
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkSkin
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPlacement
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinBone
import com.donnclab.kinetiframe.engine.rigging.model.SkinGrid
import com.donnclab.kinetiframe.engine.rigging.model.SkinMesh
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.engine.rigging.model.SkinVertex
import kotlin.math.hypot

/** Initial automatic bind for a separated neutral sketch. Only a connected joint neighbourhood
 * can share influence; spatial proximity alone would pull a wrist toward a neighbouring leg.
 * Region corrections/hidden artwork are still needed for crossing or extreme poses. */
object ActorDrawingBinder {
    fun bind(
        graph: SceneGraph,
        size: SkinSourceSize,
        placement: DrawingPlacement,
        regions: List<ActorDrawingRegion> = emptyList(),
    ): ArtworkSkin {
        val width = size.width
        val height = size.height
        val x = placement.origin.x
        val y = placement.origin.y
        val actorWidth = placement.extent.width
        val actorHeight = placement.extent.height
        ActorGraphValidation.validate(graph.copy(drawing = null))
        require(width in 1..2048 && height in 1..2048 && actorWidth > 0 && actorHeight > 0)
        require(listOf(x, y, actorWidth, actorHeight).all(Float::isFinite))
        require(
            regions.size <= 32 && regions.all {
                it.boneId in graph.bones && it.outline.size in 3..256 &&
                    it.featherPixels.isFinite() && it.featherPixels in 0f..128f &&
                    it.outline.all { p -> p.x.isFinite() && p.y.isFinite() && p.x in 0f..1f && p.y in 0f..1f }
            },
        ) { "Invalid drawing region" }
        val automatic = graph.bones.values.filter { it.length > 2f || it.parentId == null }.map { it.id }.sorted()
        val ids = (automatic + regions.map { it.boneId }).distinct().sorted()
        require(ids.size in 1..64) { "Sketch binding supports up to 64 moving parts" }
        val world = ArtworkSkinning.worldTransforms(graph)
        val columns = 32
        val rows = 32
        fun distance(px: Float, py: Float, id: String): Float {
            val m = world.getValue(id)
            val ex = m.x(graph.bones.getValue(id).length, 0f) - m.tx
            val ey = m.y(graph.bones.getValue(id).length, 0f) - m.ty
            val projection = (((px - m.tx) * ex + (py - m.ty) * ey) / (ex * ex + ey * ey)).coerceIn(0f, 1f)
            return hypot(px - m.tx - ex * projection, py - m.ty - ey * projection)
        }
        val vertices = (0..rows).flatMap { row ->
            (0..columns).map { column ->
                val px = x + actorWidth * column / columns
                val py = y + actorHeight * row / rows
                val distances = ids.associateWith { distance(px, py, it) }
                val nearest = automatic.minBy { distances.getValue(it) }
                val parent = graph.bones.getValue(nearest).parentId
                val neighbourhood = automatic.filter {
                    it == nearest || it == parent ||
                        graph.bones.getValue(it).parentId == nearest
                }
                    .sortedBy { distances.getValue(it) }.take(4)
                val radius = graph.bones.getValue(nearest).length * .25f
                val minimum = distances.getValue(nearest)
                val raw = ids.map { id ->
                    if (id !in
                        neighbourhood
                    ) {
                        0f
                    } else {
                        (1 - (distances.getValue(id) - minimum) / radius).coerceIn(0f, 1f).let {
                            it *
                                it *
                                (3 - 2 * it)
                        }
                    }
                }
                val sum = raw.sum()
                var weights = raw.map { it / sum }
                regions.forEach { region ->
                    weights = DrawingRegionWeights.blend(
                        weights,
                        ids.indexOf(region.boneId),
                        DrawingRegionWeights.influence(
                            region,
                            width.toFloat() * column / columns,
                            height.toFloat() * row / rows,
                            width,
                            height,
                        ),
                    )
                }
                SkinVertex(px, py, weights)
            }
        }
        return ArtworkSkin(
            SkinSourceSize(width, height),
            SkinGrid(columns, rows),
            SkinMesh(ids.map { SkinBone(it, world.getValue(it)) }, vertices),
        )
    }
    fun rebind(graph: SceneGraph): SceneGraph = graph.drawing?.let { drawing ->
        graph.copy(drawing = drawing.withSkin(bind(graph, drawing.source.size, drawing.placement, drawing.regions)))
    } ?: graph
}
