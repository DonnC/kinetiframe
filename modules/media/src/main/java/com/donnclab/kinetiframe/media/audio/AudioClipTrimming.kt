package com.donnclab.kinetiframe.media.audio

import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.AudioClipSourceWindow

/** Frame-aligned, non-destructive lane trimming. Never invent audio outside the selected window. */
object AudioClipTrimming {
    fun shorten(clip: AudioClipPlacement, start: Int, duration: Int, fps: Int): AudioClipPlacement {
        if (fps <= 0) return clip
        val source =
            clip.trimSource ?: AudioClipSourceWindow(clip.trimStartMicros, clip.durationFrames, clip.waveformPeaks)
        val cut = (clip.trimOffsetFrames.toLong() + start - clip.startFrame)
            .coerceIn(
                (clip.trimOffsetFrames - clip.startFrame).coerceAtLeast(0).toLong(),
                source.durationFrames.toLong() - 1,
            ).toInt()
        val kept = duration.coerceIn(1, source.durationFrames - cut)
        val from = (source.peaks.size.toLong() * cut / source.durationFrames).toInt()
        val to = (source.peaks.size.toLong() * (cut + kept) / source.durationFrames).toInt()
        return clip.copy(
            startFrame = clip.startFrame + cut - clip.trimOffsetFrames,
            durationFrames = kept,
            envelopeDurationFrames = clip.envelopeDurationFrames.takeIf { it > 0 } ?: clip.durationFrames,
            envelopeOffsetFrames = (clip.envelopeOffsetFrames + cut - clip.trimOffsetFrames).coerceAtLeast(0),
            trimStartMicros = source.inMicros + cut * 1_000_000L / fps,
            waveformPeaks = source.peaks.subList(from, to).toList(),
            trimSource = source,
            trimOffsetFrames = cut,
        )
    }
}
