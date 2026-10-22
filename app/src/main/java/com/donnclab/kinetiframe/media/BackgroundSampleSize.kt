package com.donnclab.kinetiframe.media

/** Powers of two prevent the native decoder rounding our memory limit down to a larger image. */
fun backgroundSampleSize(width: Int, height: Int, maxPixels: Long = 4_000_000, maxEdge: Int = 4096): Int {
    require(width > 0 && height > 0 && maxPixels > 0 && maxEdge > 0)
    var sample = 1
    while (true) {
        val w = (width.toLong() + sample - 1) / sample
        val h = (height.toLong() + sample - 1) / sample
        if (w * h <= maxPixels && w <= maxEdge && h <= maxEdge) return sample
        check(sample <= Int.MAX_VALUE / 2) { "Background dimensions are unsupported" }
        sample *= 2
    }
}
