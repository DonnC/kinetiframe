package com.donnclab.kinetiframe.data.export

/**
 * Bitrate selection for the H.264 video track, kept in one place so every caller uses the same
 * formula. 0.075 bits/pixel is a reasonable sweet spot for flat, line-art-style 2D animation
 * (much less noisy per-pixel detail than live-action footage), bounded so a tiny sticker-sized
 * export never starves below a usable floor and a huge canvas never balloons the file size.
 */
object ExportBitrate {
    private const val MIN_BPS = 2_000_000
    private const val MAX_BPS = 15_000_000
    private const val BITS_PER_PIXEL_PER_SECOND = 0.075

    fun bitsPerSecond(width: Int, height: Int, fps: Int): Int {
        val totalPixels = width.toLong() * height.toLong()
        val estimate = (totalPixels * fps * BITS_PER_PIXEL_PER_SECOND).toLong()
        return estimate.coerceIn(MIN_BPS.toLong(), MAX_BPS.toLong()).toInt()
    }
}
