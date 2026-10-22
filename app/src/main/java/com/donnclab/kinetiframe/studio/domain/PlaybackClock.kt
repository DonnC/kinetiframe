package com.donnclab.kinetiframe.studio.domain

/** Timeline time is elapsed time, never the number of disk renders that completed. */
class PlaybackClock(private val nanoTime: () -> Long = System::nanoTime) {
    private var startedAt = 0L
    private var startingFrame = 0
    private var fps = 24
    fun start(frame: Int, framesPerSecond: Int) {
        startingFrame = frame
        fps = framesPerSecond.coerceIn(6, 30)
        startedAt = nanoTime()
    }
    fun frame(totalFrames: Int): Int {
        val elapsed = (nanoTime() - startedAt).coerceAtLeast(0)
        val advanced = elapsed / 1_000_000_000L * fps + elapsed % 1_000_000_000L * fps / 1_000_000_000L
        return ((startingFrame + advanced) % totalFrames.coerceAtLeast(1)).toInt()
    }
    fun changeFps(newFps: Int, totalFrames: Int) = start(frame(totalFrames), newFps)
}
