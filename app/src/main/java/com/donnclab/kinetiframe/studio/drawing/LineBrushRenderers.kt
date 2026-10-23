package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.drawing.StrokeGeometry
import kotlin.math.cos
import kotlin.math.hypot

internal class PenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            PenStrokeRenderer.draw(canvas, points, width, paint)
        } finally {
            state.restore(paint)
        }
    }
}

internal class InkRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        if (points.size < 2) {
            PressureStrokeRenderer.draw(canvas, points, width, paint)
            return
        }
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.FILL
            val path = Path()
            val leftPoints = mutableListOf<PointF>()
            val rightPoints = mutableListOf<PointF>()

            StrokeGeometry.forEachSegment(points) { index, previous, current ->
                val normal = StrokeGeometry.calculateNormal(previous, current)
                val w =
                    calculatePointWidth(index + 1, points.size, current, width) *
                        BrushDynamics.inkWidthMultiplier(current.pressure)

                if (leftPoints.isEmpty()) {
                    val w0 =
                        calculatePointWidth(index, points.size, previous, width) *
                            BrushDynamics.inkWidthMultiplier(previous.pressure)
                    leftPoints.add(PointF(previous.x + normal.first * w0 / 2f, previous.y + normal.second * w0 / 2f))
                    rightPoints.add(PointF(previous.x - normal.first * w0 / 2f, previous.y - normal.second * w0 / 2f))
                }

                leftPoints.add(PointF(current.x + normal.first * w / 2f, current.y + normal.second * w / 2f))
                rightPoints.add(PointF(current.x - normal.first * w / 2f, current.y - normal.second * w / 2f))
            }

            if (leftPoints.isNotEmpty()) {
                path.moveTo(leftPoints[0].x, leftPoints[0].y)
                for (i in 1 until leftPoints.size) path.lineTo(leftPoints[i].x, leftPoints[i].y)
                for (i in rightPoints.size - 1 downTo 0) path.lineTo(rightPoints[i].x, rightPoints[i].y)
                path.close()
                canvas.drawPath(path, paint)
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class HighlighterRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            paint.strokeJoin = Paint.Join.BEVEL
            paint.alpha = BrushDynamics.highlighterAlpha(paint.alpha)
            paint.xfermode = null
            PressureStrokeRenderer.draw(canvas, points, width * 1.05f, paint)
        } finally {
            state.restore(paint)
        }
    }
}

internal class CalligraphyRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.BUTT
            paint.strokeJoin = Paint.Join.MITER
            val nibAngle = -Math.PI.toFloat() / 4f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val tangentAngle = kotlin.math.atan2(dy, dx)
                    val nibResponse = kotlin.math.abs(cos(tangentAngle - nibAngle))
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    paint.strokeWidth = dynamicWidth * (0.32f + nibResponse * 1.18f)
                    canvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint)
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class RoughPenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            var index = 0
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val nx = -dy / dist
                    val ny = dx / dist
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    val wobble = BrushDynamics.symmetricNoise(snapshot.seed, index++, 0) * dynamicWidth * 0.12f
                    paint.strokeWidth =
                        dynamicWidth * (0.82f + BrushDynamics.deterministicUnit(snapshot.seed, index, 1) * 0.28f)
                    canvas.drawLine(
                        prev.x + nx * wobble,
                        prev.y + ny * wobble,
                        curr.x + nx * wobble,
                        curr.y + ny * wobble,
                        paint,
                    )
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class GrainPenRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            PressureStrokeRenderer.draw(canvas, points, width, paint)
            paint.style = Paint.Style.FILL
            PhaseParticleRenderer(GrainProfile).draw(canvas, points, width, paint, snapshot)
        } finally {
            state.restore(paint)
        }
    }
}
