package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import kotlin.math.hypot

/** Arc-length spacing carries across event boundaries; event rate is not brush strength. */
class ManipulationDabSampler(width: Float) {
    val radius = if (width.isFinite()) (width / 2f).coerceIn(0.5f, 128f) else 1f
    private val spacing = (radius.toDouble() * 0.12).coerceIn(0.5, 3.0)
    private var remaining = spacing
    private var previousX = Float.NaN
    private var previousY = Float.NaN
    private var started = false

    fun begin(point: TouchPoint, dab: (Float, Float, Float, Float, Float) -> Unit) {
        if (started || !point.x.isFinite() || !point.y.isFinite()) return
        started = true
        previousX = point.x
        previousY = point.y
        dab(
            point.x,
            point.y,
            spacing.toFloat(),
            0f,
            if (point.pressure.isFinite()) point.pressure.coerceIn(0f, 1f) else 1f,
        )
    }

    fun segment(a: TouchPoint, b: TouchPoint, dab: (Float, Float, Float, Float, Float) -> Unit) {
        if (!a.x.isFinite() || !a.y.isFinite() || !b.x.isFinite() || !b.y.isFinite()) return
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = hypot(dx.toDouble(), dy.toDouble())
        if (previousX.isNaN()) {
            previousX = a.x
            previousY = a.y
        }
        if (distance <= 0.0 || !distance.isFinite() || distance > 1_000_000.0) return
        var travelled = remaining
        while (travelled <= distance + 1e-7) {
            val fraction = (travelled / distance).coerceAtMost(1.0).toFloat()
            val x = a.x + dx * fraction
            val y = a.y + dy * fraction
            val pressure = (a.pressure + (b.pressure - a.pressure) * fraction).let {
                if (it.isFinite()) it.coerceIn(0f, 1f) else 1f
            }
            dab(x, y, x - previousX, y - previousY, pressure)
            previousX = x
            previousY = y
            travelled += spacing
        }
        remaining = travelled - distance
    }
}
