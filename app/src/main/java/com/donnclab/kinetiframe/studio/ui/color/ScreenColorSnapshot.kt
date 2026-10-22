package com.donnclab.kinetiframe.studio.ui.color

import kotlin.math.floor
import kotlin.math.sqrt

interface ScreenColorSnapshot : AutoCloseable {
    val sourceWidth: Int
    val sourceHeight: Int
    val width: Int
    val height: Int
    fun pixel(x: Int, y: Int): Int
}
fun interface ScreenColorSource {
    suspend fun capture(): ScreenColorSnapshot
}

/** Sampling uses window coordinates, not canvas coordinates; zoom, rotation and UI offsets are already composited. */
fun sampleScreenColor(snapshot: ScreenColorSnapshot, windowX: Float, windowY: Float): Int {
    require(windowX.isFinite() && windowY.isFinite()) { "Invalid pointer position" }
    require(snapshot.sourceWidth > 0 && snapshot.sourceHeight > 0 && snapshot.width > 0 && snapshot.height > 0)
    val x = floor(windowX.toDouble() * snapshot.width / snapshot.sourceWidth).toInt().coerceIn(0, snapshot.width - 1)
    val y = floor(windowY.toDouble() * snapshot.height / snapshot.sourceHeight).toInt().coerceIn(0, snapshot.height - 1)
    return snapshot.pixel(x, y)
}

/** Native resolution when affordable; bounded 16 MiB ARGB capture on unusually large displays. */
fun screenCaptureSize(width: Int, height: Int, maxPixels: Long = 4_000_000): Pair<Int, Int> {
    require(width > 0 && height > 0 && maxPixels > 0)
    val scale = minOf(1.0, sqrt(maxPixels.toDouble() / (width.toLong() * height)))
    val targetWidth = maxOf(
        1,
        floor(width * scale).toInt(),
    ).coerceAtMost(minOf(maxPixels, Int.MAX_VALUE.toLong()).toInt())
    val targetHeight = maxOf(
        1,
        floor(height * scale).toInt(),
    ).coerceAtMost(minOf(maxPixels / targetWidth, Int.MAX_VALUE.toLong()).toInt())
    return targetWidth to targetHeight
}
