package com.donnclab.kinetiframe.studio.camera

object CameraRanges {
    fun conflicts(shot: CameraShot, shots: List<CameraShot>): List<CameraShot> = shots.filter {
        it.id != shot.id && shot.startFrame < it.endFrame && it.startFrame < shot.endFrame
    }

    /** Prefer the playhead or the next free interval, then wrap to an earlier available gap. */
    fun available(shots: List<CameraShot>, totalFrames: Int, preferred: Int, duration: Int): IntRange? {
        if (totalFrames <= 0 || shots.size >= 256) return null
        val gaps = mutableListOf<IntRange>()
        var cursor = 0
        shots.sortedBy { it.startFrame }.forEach { shot ->
            if (minOf(totalFrames, shot.startFrame) > cursor) gaps += cursor until minOf(totalFrames, shot.startFrame)
            cursor = maxOf(cursor, shot.endFrame)
        }
        if (cursor < totalFrames) gaps += cursor until totalFrames
        val wanted = preferred.coerceIn(0, totalFrames - 1)
        val gap = gaps.firstOrNull { it.last >= wanted } ?: gaps.firstOrNull() ?: return null
        val start = if (wanted in gap) wanted else gap.first
        return start until minOf(gap.last + 1, start + duration.coerceAtLeast(1))
    }
}
