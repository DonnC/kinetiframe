package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Paint
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class DashRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.BUTT
            val dash = max(width * 2.4f, 5f)
            val gap = max(width * 1.3f, 3f)
            var travelled = 0f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    var local = 0f
                    while (local < dist) {
                        val cycle = (travelled + local) % (dash + gap)
                        val take = min(dist - local, if (cycle < dash) dash - cycle else (dash + gap) - cycle)
                        if (cycle < dash && take > 0f) {
                            val t0 = local / dist
                            val t1 = (local + take) / dist
                            val dynamicWidth = calculatePointWidth(
                                idx,
                                points.size,
                                TouchPoint(
                                    prev.x + dx * t0,
                                    prev.y + dy * t0,
                                    prev.pressure,
                                    velocity = prev.velocity,
                                ),
                                width,
                            )
                            paint.strokeWidth = dynamicWidth
                            canvas.drawLine(
                                prev.x + dx * t0,
                                prev.y + dy * t0,
                                prev.x + dx * t1,
                                prev.y + dy * t1,
                                paint,
                            )
                        }
                        local += take.coerceAtLeast(0.001f)
                    }
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class GearRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            val step = max(width * 1.8f, 6f)
            var travelled = 0f
            var next = 0f
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val tx = dx / dist
                    val ty = dy / dist
                    while (next <= travelled + dist) {
                        val t = ((next - travelled) / dist).coerceIn(0f, 1f)
                        val x = prev.x + dx * t
                        val y = prev.y + dy * t
                        val dynamicWidth =
                            calculatePointWidth(
                                idx,
                                points.size,
                                TouchPoint(x, y, prev.pressure, velocity = prev.velocity),
                                width,
                            )
                        val radius =
                            dynamicWidth *
                                (0.34f + BrushDynamics.deterministicUnit(snapshot.seed, next.roundToInt(), 2) * 0.12f)
                        paint.strokeWidth = max(0.5f, dynamicWidth * 0.18f)
                        canvas.drawCircle(x, y, radius, paint)
                        canvas.drawLine(x - ty * radius, y + tx * radius, x + ty * radius, y - tx * radius, paint)
                        next += step
                    }
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}

internal class SawRenderer : BaseBrushRenderer() {
    override fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint, snapshot: BrushSnapshot) {
        val state = PaintState(paint)
        try {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.SQUARE
            var travelled = 0f
            val tooth = max(width * 1.4f, 5f)
            StrokeGeometry.forEachSegment(points) { idx, prev, curr ->
                val dx = curr.x - prev.x
                val dy = curr.y - prev.y
                val dist = hypot(dx, dy)
                if (dist > 0f) {
                    val nx = -dy / dist
                    val ny = dx / dist
                    val t0 = travelled / tooth
                    val t1 = (travelled + dist) / tooth
                    val dynamicWidth = calculatePointWidth(idx + 1, points.size, curr, width)
                    val y0 = if (kotlin.math.floor(t0).toInt() % 2 == 0) -dynamicWidth * 0.28f else dynamicWidth * 0.28f
                    val y1 = if (kotlin.math.floor(t1).toInt() % 2 == 0) -dynamicWidth * 0.28f else dynamicWidth * 0.28f
                    paint.strokeWidth = dynamicWidth * 0.15f
                    canvas.drawLine(prev.x + nx * y0, prev.y + ny * y0, curr.x + nx * y1, curr.y + ny * y1, paint)
                    travelled += dist
                }
            }
        } finally {
            state.restore(paint)
        }
    }
}
