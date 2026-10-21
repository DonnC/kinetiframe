package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.engine.model.ActorPlacementReference

/** Retain the source clock, including loop phase; a shortened block must not restart the gesture. */
object ActorPlacementTrimming {
    fun shorten(clip: ActorPlacementReference, start: Int, duration: Int): ActorPlacementReference {
        val total = (clip.durationFrames.toLong() * clip.repeatCount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val span = clip.sourceSpanFrames.takeIf { it > 0 } ?: (total + clip.sourceOffsetFrames)
        val cut = (clip.sourceOffsetFrames.toLong() + start - clip.startFrame)
            .coerceIn((clip.sourceOffsetFrames - clip.startFrame).coerceAtLeast(0).toLong(), span.toLong() - 1).toInt()
        return clip.copy(
            startFrame = clip.startFrame + cut - clip.sourceOffsetFrames,
            durationFrames = duration.coerceIn(1, span - cut),
            repeatCount = 1,
            sourceOffsetFrames = cut,
            sourceSpanFrames = span,
            sourceLoopFrames = clip.sourceLoopFrames.takeIf { it > 0 } ?: clip.durationFrames,
        )
    }
}
