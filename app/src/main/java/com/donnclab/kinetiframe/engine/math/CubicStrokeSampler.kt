package com.donnclab.kinetiframe.engine.math

import kotlin.math.abs
import kotlin.math.hypot

/** Allocation-free cubic subdivision. Control-polygon flatness bounds curve/chord error;
 * chord length also limits footprint spacing. Depth 8 bounds pathological input to 256 leaves.
 */
object CubicStrokeSampler {
    fun forEachEnd(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        emit: (Float, Float, Float) -> Unit,
    ) {
        subdivide(x0, y0, x1, y1, x2, y2, x3, y3, 0f, 1f, 0, emit)
    }

    private fun subdivide(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        t0: Float,
        t1: Float,
        depth: Int,
        emit: (Float, Float, Float) -> Unit,
    ) {
        val dx = x3 - x0
        val dy = y3 - y0
        val length = hypot(dx, dy)
        val flatness = if (length < 0.0001f) {
            maxOf(hypot(x1 - x0, y1 - y0), hypot(x2 - x0, y2 - y0))
        } else {
            maxOf(
                abs(dx * (y1 - y0) - dy * (x1 - x0)),
                abs(dx * (y2 - y0) - dy * (x2 - x0)),
            ) / length
        }
        val polygonLength = hypot(x1 - x0, y1 - y0) + hypot(x2 - x1, y2 - y1) + hypot(x3 - x2, y3 - y2)
        if (depth >= 8 || (flatness <= 0.25f && polygonLength <= 4f)) {
            emit(x3, y3, t1)
            return
        }
        val ax = (x0 + x1) * 0.5f
        val ay = (y0 + y1) * 0.5f
        val bx = (x1 + x2) * 0.5f
        val by = (y1 + y2) * 0.5f
        val cx = (x2 + x3) * 0.5f
        val cy = (y2 + y3) * 0.5f
        val dxMid = (ax + bx) * 0.5f
        val dyMid = (ay + by) * 0.5f
        val ex = (bx + cx) * 0.5f
        val ey = (by + cy) * 0.5f
        val mx = (dxMid + ex) * 0.5f
        val my = (dyMid + ey) * 0.5f
        val tm = (t0 + t1) * 0.5f
        subdivide(x0, y0, ax, ay, dxMid, dyMid, mx, my, t0, tm, depth + 1, emit)
        subdivide(mx, my, ex, ey, cx, cy, x3, y3, tm, t1, depth + 1, emit)
    }
}
