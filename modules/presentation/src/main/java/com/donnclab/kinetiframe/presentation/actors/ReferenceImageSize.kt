package com.donnclab.kinetiframe.presentation.actors

import kotlin.math.sqrt

/** Limit decoded ARGB pixels, not just file bytes; panoramas and camera images stay bounded. */
object ReferenceImageSize {
    fun target(width: Int, height: Int, heapBytes: Long): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val pixelBudget = minOf(1536L * 1536L, (heapBytes / 32L / 4L).coerceAtLeast(256L * 256L))
        val scale =
            minOf(1.0, 1536.0 / maxOf(width, height), sqrt(pixelBudget.toDouble() / (width.toDouble() * height)))
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}
