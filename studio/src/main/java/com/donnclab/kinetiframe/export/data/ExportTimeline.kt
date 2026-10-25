package com.donnclab.kinetiframe.export.data

/** Derive each timestamp from its absolute frame index; rounded intervals must not accumulate. */
object ExportTimeline {
    fun timeUs(frame: Long, fps: Int): Long {
        require(frame >= 0 && fps in 1..30)
        return Math.multiplyExact(frame, 1_000_000L) / fps
    }
}
