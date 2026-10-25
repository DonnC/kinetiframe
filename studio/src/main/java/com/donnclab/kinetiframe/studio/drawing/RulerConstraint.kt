package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Platform-independent description of an active persistent ruler.
 *
 * Coordinates are always document coordinates. UI code adapts its persisted guide
 * model into this value before a stroke begins, keeping the high-frequency stroke
 * path free of Compose and Android Canvas dependencies.
 */
enum class RulerConstraintMode { NONE, LINE, ELLIPSE, RECTANGLE, MIRROR }

data class RulerConstraintSpec(
    val mode: RulerConstraintMode,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float,
    val rotationDegrees: Float = 0f,
)

/** Stateful constraint for one physical stroke. Do not share between strokes. */
class RulerStrokeConstraintSession(private val spec: RulerConstraintSpec) {
    private var rectangleEdge: RectangleEdge? = null

    fun constrain(point: TouchPoint): TouchPoint = when (spec.mode) {
        RulerConstraintMode.NONE, RulerConstraintMode.MIRROR -> point
        RulerConstraintMode.LINE -> projectLine(point)
        RulerConstraintMode.ELLIPSE -> projectEllipse(point)
        RulerConstraintMode.RECTANGLE -> projectRectangle(point, rectangleEdge)
    }

    /**
     * Prediction is intentionally non-mutating: a guessed future point must not
     * change an edge latch that belongs to the next real sample.
     */
    fun constrainPrediction(point: TouchPoint): TouchPoint = when (spec.mode) {
        RulerConstraintMode.RECTANGLE -> projectRectangle(point, rectangleEdge)
        else -> constrain(point)
    }

    fun reflect(point: TouchPoint): TouchPoint {
        val dx = spec.endX - spec.startX
        val dy = spec.endY - spec.startY
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= EPSILON) return point
        val relativeX = point.x - spec.startX
        val relativeY = point.y - spec.startY
        val projection = (relativeX * dx + relativeY * dy) / lengthSquared
        val projectedX = spec.startX + projection * dx
        val projectedY = spec.startY + projection * dy
        return point.copy(x = 2f * projectedX - point.x, y = 2f * projectedY - point.y)
    }

    private fun projectLine(point: TouchPoint): TouchPoint {
        val dx = spec.endX - spec.startX
        val dy = spec.endY - spec.startY
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= EPSILON) return point
        val t = ((point.x - spec.startX) * dx + (point.y - spec.startY) * dy) / lengthSquared
        return point.copy(x = spec.startX + dx * t, y = spec.startY + dy * t)
    }

    private fun projectEllipse(point: TouchPoint): TouchPoint {
        val local = toLocal(point.x, point.y)
        val rx = spec.radiusX.coerceAtLeast(EPSILON)
        val ry = spec.radiusY.coerceAtLeast(EPSILON)
        val angle = atan2(local.y / ry, local.x / rx)
        return pointAtLocal(cos(angle) * rx, sin(angle) * ry, point)
    }

    private fun projectRectangle(point: TouchPoint, retainedEdge: RectangleEdge? = null): TouchPoint {
        val local = toLocal(point.x, point.y)
        val halfWidth = spec.radiusX.coerceAtLeast(EPSILON)
        val halfHeight = spec.radiusY.coerceAtLeast(EPSILON)
        val nearest = listOf(
            RectangleEdge.LEFT to abs(local.x + halfWidth),
            RectangleEdge.RIGHT to abs(local.x - halfWidth),
            RectangleEdge.TOP to abs(local.y + halfHeight),
            RectangleEdge.BOTTOM to abs(local.y - halfHeight),
        ).minByOrNull { it.second }!!.first
        val edge =
            retainedEdge?.takeIf {
                distanceTo(it, local, halfWidth, halfHeight) <=
                    distanceTo(nearest, local, halfWidth, halfHeight) + EDGE_HYSTERESIS
            }
                ?: nearest
        rectangleEdge = edge
        val constrainedX = when (edge) {
            RectangleEdge.LEFT -> -halfWidth
            RectangleEdge.RIGHT -> halfWidth
            else -> local.x.coerceIn(-halfWidth, halfWidth)
        }
        val constrainedY = when (edge) {
            RectangleEdge.TOP -> -halfHeight
            RectangleEdge.BOTTOM -> halfHeight
            else -> local.y.coerceIn(-halfHeight, halfHeight)
        }
        return pointAtLocal(constrainedX, constrainedY, point)
    }

    private fun distanceTo(edge: RectangleEdge, point: LocalPoint, halfWidth: Float, halfHeight: Float): Float =
        when (edge) {
            RectangleEdge.LEFT -> abs(point.x + halfWidth)
            RectangleEdge.RIGHT -> abs(point.x - halfWidth)
            RectangleEdge.TOP -> abs(point.y + halfHeight)
            RectangleEdge.BOTTOM -> abs(point.y - halfHeight)
        }

    private fun toLocal(x: Float, y: Float): LocalPoint {
        val radians = Math.toRadians(spec.rotationDegrees.toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()
        val dx = x - spec.centerX
        val dy = y - spec.centerY
        return LocalPoint(dx * cosine + dy * sine, -dx * sine + dy * cosine)
    }

    private fun pointAtLocal(x: Float, y: Float, source: TouchPoint): TouchPoint {
        val radians = Math.toRadians(spec.rotationDegrees.toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()
        return source.copy(
            x = spec.centerX + x * cosine - y * sine,
            y = spec.centerY + x * sine + y * cosine,
        )
    }

    private data class LocalPoint(val x: Float, val y: Float)
    private enum class RectangleEdge { LEFT, RIGHT, TOP, BOTTOM }

    private companion object {
        const val EPSILON = 0.0001f
        const val EDGE_HYSTERESIS = 12f
    }
}
