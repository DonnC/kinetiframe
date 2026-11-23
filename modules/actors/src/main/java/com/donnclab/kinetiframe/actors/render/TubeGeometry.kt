package com.donnclab.kinetiframe.actors.render

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.acos
import kotlin.math.atan2

/**
 * A tube through [points] (a bone chain's joints): straight runs joined by round corners, so it
 * bends like a sleeve, a leg or a tail instead of hinging like separate cutouts. Width runs from
 * [startWidth] to [endWidth] along the chain.
 */
internal class TubeGeometry(
    private val points: List<Point2>,
    private val startWidth: Float,
    private val endWidth: Float,
    private val cuffed: Boolean,
) {
    private val lengths = points.zipWithNext { a, b -> (b - a).getDistance().coerceAtLeast(EPSILON) }
    private val total = lengths.sum()
    private val start = points.first()
    private val end = points.last()
    private val u = (end - points[points.size - 2]) / lengths.last()
    private val n = Point2(-u.y, u.x)
    private val endHalf = endWidth / 2f

    private fun widthAt(distance: Float) = startWidth + (endWidth - startWidth) * (distance / total)

    fun outline(): Path {
        val outline = Path()
        if (startWidth == endWidth) {
            stroke(centre(0, points.size - 1), startWidth, outline)
        } else {
            // A tapering tube: each run gets the width at its middle, joined by round joints.
            var travelled = 0f
            lengths.forEachIndexed { index, length ->
                val run = Path()
                stroke(centre(index, index + 1), widthAt(travelled + length / 2f), run)
                outline.op(run, Path.Op.UNION)
                travelled += length
                val joint = points[index + 1]
                outline.op(circle(joint, widthAt(travelled) / 2f), Path.Op.UNION)
            }
        }
        outline.op(circle(start, startWidth / 2f), Path.Op.UNION)
        if (cuffed) {
            // A slightly rounded cuff mouth.
            val lip = Path().apply {
                addOval(RectF(end.x - 5f, end.y - endHalf, end.x + 5f, end.y + endHalf), Path.Direction.CW)
                transform(Matrix().apply { setRotate(degrees(u), end.x, end.y) })
            }
            outline.op(lip, Path.Op.UNION)
        } else {
            outline.op(circle(end, endHalf), Path.Op.UNION)
        }
        return outline
    }

    /** The root circle inside the body's shape, minus the rest of the chain, which is always inked. */
    fun hiddenRoot(body: Path): Path {
        val hidden = circle(start, startWidth * 0.62f)
        hidden.op(body, Path.Op.INTERSECT)
        if (points.size > 2) {
            val rest = Path()
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = maxOf(startWidth, endWidth) + 10f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }.getFillPath(
                Path().apply {
                    moveTo(points[1].x, points[1].y)
                    points.drop(2).forEach { lineTo(it.x, it.y) }
                    lineTo(end.x + u.x * 8f, end.y + u.y * 8f)
                },
                rest,
            )
            hidden.op(rest, Path.Op.DIFFERENCE)
        }
        return hidden
    }

    /** A fold on the inside of every joint bent more than 35 degrees. */
    fun folds(): List<Path> {
        var travelled = 0f
        return (1 until points.size - 1).mapNotNull { index ->
            travelled += lengths[index - 1]
            val v = (points[index] - points[index - 1]) / lengths[index - 1]
            val w = (points[index + 1] - points[index]) / lengths[index]
            val bend = Math.toDegrees(acos((v.x * w.x + v.y * w.y).coerceIn(-1f, 1f).toDouble()))
            val half = widthAt(travelled) / 2f
            if (bend <= 35.0 || lengths[index - 1] <= half * 2f) return@mapNotNull null
            var n1 = Point2(-v.y, v.x)
            if (n1.x * w.x + n1.y * w.y < 0f) n1 = Point2(-n1.x, -n1.y)
            val b = points[index] - v * 10f
            Path().apply {
                moveTo(b.x + n1.x * (half - 1f), b.y + n1.y * (half - 1f))
                quadTo(
                    b.x + n1.x * half * 0.62f - v.x * 3f,
                    b.y + n1.y * half * 0.62f - v.y * 3f,
                    b.x + n1.x * half * 0.38f - v.x * 8f,
                    b.y + n1.y * half * 0.38f - v.y * 8f,
                )
            }
        }
    }

    /** The cuff's fill, just inside the tube's outline so the side lines stay whole. */
    fun cuffBand(line: Float): Path {
        val f = endHalf - line / 2f
        val c = end - u * 13f
        return Path().apply {
            moveTo(c.x + n.x * f, c.y + n.y * f)
            lineTo(end.x + n.x * f, end.y + n.y * f)
            cubicTo(
                end.x + n.x * f + u.x * 7f,
                end.y + n.y * f + u.y * 7f,
                end.x - n.x * f + u.x * 7f,
                end.y - n.y * f + u.y * 7f,
                end.x - n.x * f,
                end.y - n.y * f,
            )
            lineTo(c.x - n.x * f, c.y - n.y * f)
            quadTo(c.x + u.x * 4f, c.y + u.y * 4f, c.x + n.x * f, c.y + n.y * f)
            close()
        }
    }

    fun cuffMouth(): Path = Path().apply {
        moveTo(end.x + n.x * endHalf, end.y + n.y * endHalf)
        cubicTo(
            end.x + n.x * endHalf + u.x * 7f,
            end.y + n.y * endHalf + u.y * 7f,
            end.x - n.x * endHalf + u.x * 7f,
            end.y - n.y * endHalf + u.y * 7f,
            end.x - n.x * endHalf,
            end.y - n.y * endHalf,
        )
    }

    fun cuffSeam(): Path {
        val c = end - u * 13f
        return Path().apply {
            moveTo(c.x + n.x * (endHalf - 1f), c.y + n.y * (endHalf - 1f))
            quadTo(c.x + u.x * 4f, c.y + u.y * 4f, c.x - n.x * (endHalf - 1f), c.y - n.y * (endHalf - 1f))
        }
    }

    /** The centre line from joint [from] to joint [to], rounding every corner in between. */
    private fun centre(from: Int, to: Int): Path = Path().apply {
        moveTo(points[from].x, points[from].y)
        for (index in from + 1 until to) {
            val joint = points[index]
            val r = minOf(22f, 0.45f * minOf(lengths[index - 1], lengths[index]))
            val v = (joint - points[index - 1]) / lengths[index - 1]
            val w = (points[index + 1] - joint) / lengths[index]
            lineTo(joint.x - v.x * r, joint.y - v.y * r)
            quadTo(joint.x, joint.y, joint.x + w.x * r, joint.y + w.y * r)
        }
        lineTo(points[to].x, points[to].y)
    }

    private fun stroke(centre: Path, width: Float, into: Path) {
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.BUTT
            strokeJoin = Paint.Join.ROUND
        }.getFillPath(centre, into)
    }

    private fun circle(at: Point2, radius: Float) = Path().apply { addCircle(at.x, at.y, radius, Path.Direction.CW) }

    private fun degrees(direction: Point2) = Math.toDegrees(atan2(direction.y, direction.x).toDouble()).toFloat()

    private companion object {
        const val EPSILON = 0.001f
    }
}
