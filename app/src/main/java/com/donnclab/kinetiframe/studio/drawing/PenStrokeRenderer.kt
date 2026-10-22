package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.donnclab.kinetiframe.project.model.TouchPoint

/** Ballpoint-style pen: fixed width, native analytic curves, one alpha deposit.
 * Pencil/ink brushes still own pressure dynamics; raw force/speed must not roughen the Pen. */
object PenStrokeRenderer {
    private val scratch = ThreadLocal.withInitial { Path() }
    fun prepareThread() {
        scratch.get()
    }

    fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint) {
        if (points.isEmpty()) return
        val path = requireNotNull(scratch.get())
        path.rewind()
        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until points.lastIndex) appendSegment(path, points, i)
        finishTip(path, points.last())
        drawPath(canvas, path, points, width, paint)
    }

    /** Midpoint quadratics have no future control-point revisions and stay in their convex hull.
     * A deliberate sharp corner is a line join, not an overshooting spline or rounded detour. */
    fun appendSegment(path: Path, points: List<TouchPoint>, index: Int) {
        val control = points[index]
        val next = points[index + 1]
        val mx = (control.x + next.x) * 0.5f
        val my = (control.y + next.y) * 0.5f
        val previous = points.getOrNull(index - 1)
        if (previous != null && isCorner(previous, control, next)) {
            path.lineTo(control.x, control.y)
            path.lineTo(mx, my)
        } else {
            path.quadTo(control.x, control.y, mx, my)
        }
    }

    fun finishTip(path: Path, point: TouchPoint) {
        path.lineTo(point.x, point.y)
    }

    fun drawPath(canvas: Canvas, path: Path, points: List<TouchPoint>, width: Float, paint: Paint) {
        if (points.isEmpty() || !width.isFinite() || width <= 0f) return
        val style = paint.style
        val oldWidth = paint.strokeWidth
        val cap = paint.strokeCap
        val join = paint.strokeJoin
        try {
            paint.strokeWidth = width
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            // Some imported/contact streams contain several coincident points. A zero-length
            // stroked curve is rasterizer-dependent; a tap must always use the same disk.
            if (points.size == 1 || points.all { it.x == points[0].x && it.y == points[0].y }) {
                paint.style = Paint.Style.FILL
                canvas.drawCircle(points[0].x, points[0].y, width * 0.5f, paint)
            } else {
                paint.style = Paint.Style.STROKE
                canvas.drawPath(path, paint)
            }
        } finally {
            paint.style = style
            paint.strokeWidth = oldWidth
            paint.strokeCap = cap
            paint.strokeJoin = join
        }
    }

    private fun isCorner(a: TouchPoint, b: TouchPoint, c: TouchPoint): Boolean {
        val ux = b.x - a.x
        val uy = b.y - a.y
        val vx = c.x - b.x
        val vy = c.y - b.y
        val length = kotlin.math.hypot(ux, uy) * kotlin.math.hypot(vx, vy)
        return length > 0.0001f && (ux * vx + uy * vy) / length < 0.35f
    }
}
