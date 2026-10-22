package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.project.model.AudioClipPlacement

/** Overlaps the next clip, never stretches or resamples speech. Pair uses sin/cos gain law. */
object AudioCrossfade {
    fun apply(
        first: AudioClipPlacement,
        second: AudioClipPlacement,
        frames: Int,
    ): Pair<AudioClipPlacement, AudioClipPlacement> {
        require(
            first.clipId != second.clipId && first.trackIndex == second.trackIndex &&
                second.startFrame >= first.startFrame + first.durationFrames,
        )
        val overlap = frames.coerceIn(1, minOf(first.durationFrames, second.durationFrames))
        return first.copy(
            fadeOutFrames = overlap,
            equalPowerFades = true,
            envelopeOffsetFrames = 0,
            envelopeDurationFrames = first.durationFrames,
        ) to
            second.copy(
                startFrame = first.startFrame + first.durationFrames - overlap,
                fadeInFrames = overlap,
                equalPowerFades = true,
                envelopeOffsetFrames = 0,
                envelopeDurationFrames = second.durationFrames,
            )
    }
}
