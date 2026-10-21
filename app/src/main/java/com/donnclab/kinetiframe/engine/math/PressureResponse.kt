package com.donnclab.kinetiframe.engine.math

import kotlin.math.exp

/** Time-domain pressure response; equal elapsed time produces equal response at any sample rate. */
object PressureResponse {
    private const val TIME_CONSTANT_SECONDS = 0.037f

    fun filter(previous: Float?, pressure: Float, elapsedSeconds: Float): Float {
        val target = if (pressure.isFinite()) pressure.coerceIn(0f, 1f) else previous ?: 1f
        if (previous == null) return target
        val elapsed = if (elapsedSeconds.isFinite()) elapsedSeconds.coerceAtLeast(0f) else 0f
        return previous + (target - previous) * (1f - exp(-elapsed / TIME_CONSTANT_SECONDS))
    }

    /** Finger/unknown contacts do not provide calibrated force and must not alter line width. */
    fun contactPressure(pressure: Float, isStylus: Boolean): Float =
        if (isStylus && pressure.isFinite()) pressure.coerceIn(0f, 1f) else 1f
}
