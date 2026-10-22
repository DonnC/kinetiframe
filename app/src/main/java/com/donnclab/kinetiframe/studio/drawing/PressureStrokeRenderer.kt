package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.drawing.StrokeGeometry
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Round contact footprints joined by variable-width quads, filled once per stroke.
 * All contours have the same winding: overlaps (including reversals) are a union, not holes
 * or repeated alpha deposits. Coincident samples remain disks instead of degenerate envelopes.
 */
object PressureStrokeRenderer {
    private val scratch = ThreadLocal.withInitial { Path() }

    /** Called during gesture-holder creation, before any Compose draw callback. */
    fun prepareThread() {
        scratch.get()
    }

    fun draw(canvas: Canvas, points: List<TouchPoint>, baseWidth: Float, paint: Paint, startSourceSegment: Int = 0) {
        if (points.isEmpty()) return
        val path = requireNotNull(scratch.get())
        path.rewind()
        appendStroke(path, points, baseWidth, startSourceSegment)
        drawPath(canvas, path, paint)
    }

    fun drawPath(canvas: Canvas, path: Path, paint: Paint) {
        val original = paint.style
        try {
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
        } finally {
            paint.style = original
        }
    }

    fun appendStroke(
        path: Path,
        points: List<TouchPoint>,
        baseWidth: Float,
        startSourceSegment: Int = 0,
        prediction: TouchPoint? = null,
    ) {
        if (points.isEmpty()) return
        val count = points.size + if (prediction == null) 0 else 1
        fun sample(index: Int): TouchPoint = if (index == points.size) requireNotNull(prediction) else points[index]
        if (count == 1) {
            appendContact(path, points.first(), baseWidth)
            return
        }
        for (i in startSourceSegment.coerceAtLeast(0) until count - 1) {
            appendSourceSegment(
                path,
                sample(max(0, i - 1)),
                sample(i),
                sample(i + 1),
                sample(min(count - 1, i + 2)),
                baseWidth,
            )
        }
    }

    fun appendSourceSegment(
        path: Path,
        p0: TouchPoint,
        p1: TouchPoint,
        p2: TouchPoint,
        p3: TouchPoint,
        baseWidth: Float,
    ) {
        appendContact(path, p1, baseWidth)
        var previous = p1
        StrokeGeometry.forEachInterpolatedEnd(p0, p1, p2, p3) { current ->
            val dx = current.x - previous.x
            val dy = current.y - previous.y
            val length = hypot(dx, dy)
            val r0 = radius(previous, baseWidth)
            val r1 = radius(current, baseWidth)
            if (length > 0.0001f) {
                val nx = -dy / length
                val ny = dx / length
                path.moveTo(previous.x - nx * r0, previous.y - ny * r0)
                path.lineTo(current.x - nx * r1, current.y - ny * r1)
                path.lineTo(current.x + nx * r1, current.y + ny * r1)
                path.lineTo(previous.x + nx * r0, previous.y + ny * r0)
                path.close()
            }
            appendContact(path, current, baseWidth)
            previous = current
        }
    }

    private fun radius(point: TouchPoint, width: Float): Float =
        StrokeGeometry.calculateDynamicWidth(width, point.pressure, point.velocity) * 0.5f

    private fun appendContact(path: Path, point: TouchPoint, width: Float) {
        path.addCircle(point.x, point.y, radius(point, width), Path.Direction.CW)
    }
}
