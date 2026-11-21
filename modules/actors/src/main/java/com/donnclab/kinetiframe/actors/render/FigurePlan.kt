package com.donnclab.kinetiframe.actors.render

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.donnclab.kinetiframe.actors.rig.math.ComputedBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.figure.DrawnFigure
import com.donnclab.kinetiframe.core.actor.figure.FigurePart
import com.donnclab.kinetiframe.core.actor.figure.FigurePath
import com.donnclab.kinetiframe.core.actor.figure.FigureSurface
import com.donnclab.kinetiframe.core.actor.figure.FigureTube
import com.donnclab.kinetiframe.core.actor.figure.PathCommand
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.pow

/** One filled or inked curve of a drawn figure, in the actor's own space. */
class FigureStroke(
    val path: Path,
    val fill: Int?,
    val lineWidth: Float,
    val ink: Int,
    /** Where this outline must not be inked, such as the shoulder inside the shirt. */
    val hiddenOutline: Path? = null,
)

class FigureDrawing(val strokes: List<FigureStroke>) {
    val bounds: RectF = RectF().also { all ->
        val one = RectF()
        strokes.forEachIndexed { index, stroke ->
            stroke.path.computeBounds(one, true)
            if (index == 0) all.set(one) else all.union(one)
        }
    }
}

/**
 * Builds a figure's curves for one pose, for any kind of actor. Painting order follows how an
 * animator inks a limb: the tube, then whatever grows from its end (a hand, a paw), then the
 * cuff over the join, so a hand is never painted over by its own sleeve.
 */
object FigurePlan {
    fun build(figure: DrawnFigure, world: Map<String, ComputedBone>): FigureDrawing {
        val ink = colour(figure, figure.ink)
        val strokes = mutableListOf<FigureStroke>()
        val endParts = figure.tubes.associate { tube -> tube.id to partsAtEnd(figure, tube, world) }
        val drawnWithTube = endParts.values.flatten().map { it.id }.toSet()
        val pieces = figure.tubes.map { it.drawOrder to it } +
            figure.parts.filter { it.id !in drawnWithTube }.map { it.drawOrder to it } +
            figure.surfaces.map { it.drawOrder to it }
        pieces.sortedBy { it.first }.forEach { (_, piece) ->
            when (piece) {
                is FigureTube -> tube(figure, piece, world, ink, endParts.getValue(piece.id), strokes)
                is FigurePart -> part(figure, piece, world, ink, strokes)
                is FigureSurface -> surface(figure, piece, world, ink, strokes)
            }
        }
        return FigureDrawing(strokes)
    }

    /** Parts on a bone that starts where the tube ends: a hand on an arm, a paw on a leg. */
    private fun partsAtEnd(figure: DrawnFigure, tube: FigureTube, world: Map<String, ComputedBone>): List<FigurePart> {
        val end = world[tube.boneIds.last()]?.endPoint ?: return emptyList()
        return figure.parts.filter { part ->
            part.boneId !in tube.boneIds &&
                world[part.boneId]?.let { (it.startPoint - end).getDistance() < 0.5f } == true
        }
    }

    private fun tube(
        figure: DrawnFigure,
        tube: FigureTube,
        world: Map<String, ComputedBone>,
        ink: Int,
        endParts: List<FigurePart>,
        strokes: MutableList<FigureStroke>,
    ) {
        val bones = tube.boneIds.map { world[it] ?: return }
        val points = listOf(bones.first().startPoint) + bones.map { it.endPoint }
        val geometry = TubeGeometry(points, tube.width, tube.endWidth, tube.cuff)
        val fill = colour(figure, tube.fill)
        val hidden = tube.rootPartId?.let { id -> figure.parts.firstOrNull { it.id == id } }
            ?.let { root -> geometry.hiddenRoot(partFill(figure, root, world)) }
        strokes += FigureStroke(geometry.outline(), fill, figure.lineWidth, ink, hidden)
        geometry.folds().forEach { strokes += FigureStroke(it, null, figure.lineWidth * 0.75f, ink) }
        endParts.forEach { part(figure, it, world, ink, strokes) }
        if (tube.cuff) {
            strokes += FigureStroke(geometry.cuffBand(figure.lineWidth), fill, 0f, ink)
            strokes += FigureStroke(geometry.cuffMouth(), null, figure.lineWidth, ink)
            strokes += FigureStroke(geometry.cuffSeam(), null, figure.lineWidth * 0.75f, ink)
        }
    }

    private fun part(
        figure: DrawnFigure,
        part: FigurePart,
        world: Map<String, ComputedBone>,
        ink: Int,
        strokes: MutableList<FigureStroke>,
    ) {
        val matrix = partMatrix(part, world) ?: return
        part.shapes(figure.selections[part.boneId]).forEach { shape ->
            val path = toPath(FigurePath.parse(shape.path)).apply { transform(matrix) }
            strokes += FigureStroke(path, shape.fill?.let { colour(figure, it) }, figure.lineWidth * shape.line, ink)
        }
    }

    private fun surface(
        figure: DrawnFigure,
        surface: FigureSurface,
        world: Map<String, ComputedBone>,
        ink: Int,
        strokes: MutableList<FigureStroke>,
    ) {
        val skin = SurfaceSkin(surface, world)
        surface.shapes.forEach { shape ->
            val path = toPath(FigurePath.parse(shape.path), skin::map)
            strokes += FigureStroke(path, shape.fill?.let { colour(figure, it) }, figure.lineWidth * shape.line, ink)
        }
    }

    private fun partFill(figure: DrawnFigure, part: FigurePart, world: Map<String, ComputedBone>): Path {
        val result = Path()
        val matrix = partMatrix(part, world) ?: return result
        part.shapes(figure.selections[part.boneId]).filter { it.fill != null }.forEach { shape ->
            result.op(toPath(FigurePath.parse(shape.path)).apply { transform(matrix) }, Path.Op.UNION)
        }
        return result
    }

    private fun partMatrix(part: FigurePart, world: Map<String, ComputedBone>): Matrix? {
        val bone = world[part.boneId] ?: return null
        return Matrix(bone.worldMatrix).apply { if (part.mirror) preScale(1f, -1f) }
    }

    fun toPath(commands: List<PathCommand>, map: (Float, Float) -> Point2 = ::Point2): Path = Path().apply {
        commands.forEach { command ->
            when (command) {
                is PathCommand.MoveTo -> map(command.x, command.y).let { moveTo(it.x, it.y) }

                is PathCommand.LineTo -> map(command.x, command.y).let { lineTo(it.x, it.y) }

                is PathCommand.QuadTo -> {
                    val c = map(command.cx, command.cy)
                    val p = map(command.x, command.y)
                    quadTo(c.x, c.y, p.x, p.y)
                }

                is PathCommand.CubicTo -> {
                    val c1 = map(command.c1x, command.c1y)
                    val c2 = map(command.c2x, command.c2y)
                    val p = map(command.x, command.y)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, p.x, p.y)
                }

                PathCommand.Close -> close()
            }
        }
    }

    private fun colour(figure: DrawnFigure, name: String): Int = Color.parseColor(figure.palette.getValue(name))
}

/**
 * Moves rest-pose points with the bones a surface follows. Each point's weights come from its
 * rest distance to each bone, so points near a wing finger follow that finger.
 */
private class SurfaceSkin(private val surface: FigureSurface, world: Map<String, ComputedBone>) {
    private val bound = surface.bones.mapNotNull { bone ->
        val posed = world[bone.boneId]?.worldMatrix ?: return@mapNotNull null
        val values = FloatArray(9).also(posed::getValues)
        val now = SkinAffine(Point2(values[0], values[1]), Point2(values[3], values[4]), Point2(values[2], values[5]))
        Bound(bone.bindWorld, now.times(bone.bindWorld.inverse()), bone.length)
    }

    fun map(x: Float, y: Float): Point2 {
        if (bound.isEmpty()) return Point2(x, y)
        var total = 0f
        var px = 0f
        var py = 0f
        bound.forEach { bone ->
            val weight = 1f / (1f + distance(x, y, bone)).pow(surface.falloff)
            total += weight
            px += weight * bone.motion.x(x, y)
            py += weight * bone.motion.y(x, y)
        }
        return Point2(px / total, py / total)
    }

    private fun distance(x: Float, y: Float, bone: Bound): Float {
        val ox = bone.bind.tx
        val oy = bone.bind.ty
        val dx = bone.bind.a * bone.length
        val dy = bone.bind.c * bone.length
        val lengthSquared = (dx * dx + dy * dy).coerceAtLeast(0.0001f)
        val t = (((x - ox) * dx + (y - oy) * dy) / lengthSquared).coerceIn(0f, 1f)
        return Point2(x - ox - dx * t, y - oy - dy * t).getDistance()
    }

    private class Bound(val bind: SkinAffine, val motion: SkinAffine, val length: Float)
}
