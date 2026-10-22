package com.donnclab.kinetiframe.studio.camera

/** Keep a shot attached to its scene frames as drawings are inserted/deleted. Empty shots drop. */
object CameraFrameStructure {
    fun reindex(shots: List<CameraShot>, inserted: Int?, deleted: List<Int>): List<CameraShot> {
        val cuts = deleted.distinct()
        return shots.mapNotNull { shot ->
            var start = shot.startFrame
            var end = shot.endFrame
            if (inserted != null) {
                if (inserted <= start) {
                    start++
                    end++
                } else if (inserted < end) {
                    end++
                }
            }
            val from = start - cuts.count { it < start }
            val to = end - cuts.count { it < end }
            if (to <= from) null else shot.copy(startFrame = from, endFrame = to)
        }
    }
}
