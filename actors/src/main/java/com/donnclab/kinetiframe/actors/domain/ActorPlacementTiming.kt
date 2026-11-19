package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.document.ActorPlacement
import com.donnclab.kinetiframe.core.document.ActorPlacementReference

/** Maps a project frame to a local performance time without evaluating rig data. */
object ActorPlacementTiming {
    fun elapsedMicros(placement: ActorPlacementReference, frame: Int, fps: Int): Long? = elapsedMicros(
        frame.toLong() - placement.startFrame,
        placement.durationFrames,
        placement.repeatCount,
        placement.sourceOffsetFrames,
        placement.sourceLoopFrames,
        fps,
    )

    /** Durable placement equivalent used by project-file resolution. */
    fun elapsedMicros(placement: ActorPlacement, frame: Int, fps: Int): Long? = elapsedMicros(
        frame.toLong() - placement.startFrame,
        placement.durationFrames,
        placement.repeatCount,
        placement.sourceOffsetFrames,
        placement.sourceLoopFrames,
        fps,
    )

    /** [elapsedFrames] counts from the placement's start frame; it is negative before the start. */
    private fun elapsedMicros(
        elapsedFrames: Long,
        durationFrames: Int,
        repeatCount: Int,
        sourceOffsetFrames: Int,
        sourceLoopFrames: Int,
        fps: Int,
    ): Long? {
        if (fps <= 0 || elapsedFrames < 0) return null
        val totalFrames = durationFrames.toLong() * repeatCount
        if (elapsedFrames !in 0 until totalFrames) return null
        // Each repeat restarts the authored clip. Playback mode decides loop/hold inside it.
        val frameInRepeat =
            (elapsedFrames + sourceOffsetFrames) % (sourceLoopFrames.takeIf { it > 0 } ?: durationFrames)
        return frameInRepeat * 1_000_000L / fps
    }
}
