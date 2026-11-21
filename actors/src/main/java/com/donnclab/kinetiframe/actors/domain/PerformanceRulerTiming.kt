package com.donnclab.kinetiframe.actors.domain

import kotlin.math.roundToInt

/** Convert from absolute time each time; multiplying a truncated frame period accumulates drift. */
object PerformanceRulerTiming {
    fun frameAt(timeMicros: Long, fps: Int): Int =
        (timeMicros.coerceAtLeast(0) * fps.coerceAtLeast(1) / 1_000_000.0).roundToInt()

    fun timeAt(frame: Int, fps: Int, durationMicros: Long): Long =
        (frame.toLong() * 1_000_000L / fps.coerceAtLeast(1)).coerceIn(0, durationMicros)

    fun frameCount(durationMicros: Long, fps: Int): Int =
        kotlin.math.ceil(durationMicros * fps.coerceAtLeast(1) / 1_000_000.0).toInt().coerceAtLeast(1)
}
