package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.AudioClipSourceWindow
import com.donnclab.kinetiframe.project.model.VideoClipPlacement

/** One frame clock for linked picture and sound. Audio retains its independent volume/fades. */
object VideoAudioLink {
    fun synchronize(video: VideoClipPlacement, audio: AudioClipPlacement, fps: Int): AudioClipPlacement {
        require(fps > 0)
        val original =
            audio.trimSource
                ?: AudioClipSourceWindow(
                    0,
                    video.sourceDurationFrames.takeIf {
                        it > 0
                    } ?: audio.durationFrames,
                    audio.waveformPeaks,
                )
        val cut = video.sourceOffsetFrames
        val count = video.timelineEndFrame - video.timelineStartFrame
        val first = (original.peaks.size.toLong() * cut / original.durationFrames).toInt().coerceIn(
            0,
            original.peaks.size,
        )
        val last = (original.peaks.size.toLong() * (cut + count) / original.durationFrames).toInt().coerceIn(
            first,
            original.peaks.size,
        )
        return audio.copy(
            startFrame = video.timelineStartFrame,
            durationFrames = video.timelineEndFrame - video.timelineStartFrame,
            trimStartMicros = (video.videoSourceOffsetSeconds * 1_000_000).toLong(),
            trimOffsetFrames = video.sourceOffsetFrames,
            envelopeOffsetFrames = (audio.envelopeOffsetFrames + cut - audio.trimOffsetFrames).coerceAtLeast(0),
            envelopeDurationFrames = audio.envelopeDurationFrames.takeIf { it > 0 } ?: audio.durationFrames,
            trimSource = original,
            waveformPeaks = original.peaks.subList(first, last),
        )
    }
}
