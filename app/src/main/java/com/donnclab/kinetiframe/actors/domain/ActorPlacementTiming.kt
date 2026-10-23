package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorPlacement
import com.donnclab.kinetiframe.core.document.ActorPlacementReference

/** Maps a project frame to a local performance time without evaluating rig data. */
object ActorPlacementTiming {
    fun elapsedMicros(placement: ActorPlacementReference, frame: Int, fps: Int): Long? = elapsedMicros(
        placement.startFrame,
        placement.durationFrames,
        placement.repeatCount,
        frame,
        fps,
        placement.sourceOffsetFrames,
        placement.sourceLoopFrames,
    )

    /** Durable placement equivalent used by project-file resolution. */
    fun elapsedMicros(placement: ActorPlacement, frame: Int, fps: Int): Long? = elapsedMicros(
        placement.startFrame,
        placement.durationFrames,
        placement.repeatCount,
        frame,
        fps,
        placement.sourceOffsetFrames,
        placement.sourceLoopFrames,
    )

    private fun elapsedMicros(
        startFrame: Int,
        durationFrames: Int,
        repeatCount: Int,
        frame: Int,
        fps: Int,
        sourceOffsetFrames: Int,
        sourceLoopFrames: Int,
    ): Long? {
        if (fps <= 0 || frame < startFrame) return null
        val elapsedFrames = frame.toLong() - startFrame
        val totalFrames = durationFrames.toLong() * repeatCount
        if (elapsedFrames !in 0 until totalFrames) return null
        // Each repeat restarts the authored clip. Playback mode decides loop/hold inside it.
        val frameInRepeat =
            (elapsedFrames + sourceOffsetFrames) % (sourceLoopFrames.takeIf { it > 0 } ?: durationFrames)
        return frameInRepeat * 1_000_000L / fps
    }
}
