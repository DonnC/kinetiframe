package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.project.model.AudioClipPlacement

/**
 * Flattens every placed audio clip into one peak per animation frame, for the thin waveform ribbon
 * that runs above the frame filmstrip.
 *
 * The ribbon answers a single question while scrubbing -- "is there sound here, and how loud?" --
 * so lanes mix down into one outline rather than being drawn separately. That is also why a frame
 * covered by several clips takes the loudest peak rather than a sum: the ribbon is a guide, not a
 * mix meter, and summing would clip to a flat ceiling wherever two clips overlap, hiding exactly
 * the structure the animator is scanning for.
 *
 * Muted lanes are excluded. A muted clip will not be heard at that frame, so showing its outline
 * would advertise audio that is not going to play.
 *
 * Pure and Android-free, so the frame-to-peak mapping is directly unit-testable.
 */
object TimelineAudioSummary {

    /**
     * One 0f..1f peak per frame in `0 until totalFrames`. Frames with no audible clip are 0f.
     *
     * Returns empty for a non-positive [totalFrames] rather than allocating a degenerate list.
     */
    fun peaksPerFrame(
        clips: Collection<AudioClipPlacement>,
        totalFrames: Int,
        mutedTracks: Set<Int> = emptySet(),
    ): List<Float> = window(clips, 0, totalFrames, mutedTracks)

    /** Visible-window allocation, volume-aware and peak-preserving for short speech transients. */
    fun window(
        clips: Collection<AudioClipPlacement>,
        firstFrame: Int,
        count: Int,
        mutedTracks: Set<Int> = emptySet(),
        samplesPerFrame: Int = 1,
    ): List<Float> {
        require(samplesPerFrame in 1..32)
        if (count <= 0) return emptyList()
        val result = FloatArray(count * samplesPerFrame)
        for (clip in clips) {
            if (clip.trackIndex in mutedTracks || clip.volume == 0f) continue
            val source = clip.trimSource
            val peaks = source?.peaks ?: clip.waveformPeaks
            if (peaks.isEmpty()) continue
            val duration = source?.durationFrames ?: clip.durationFrames
            val sourceStart = clip.startFrame - if (source != null) clip.trimOffsetFrames else 0
            val start = maxOf(firstFrame, clip.startFrame)
            val end = minOf(firstFrame + count, clip.startFrame + clip.durationFrames)
            for (frame in start until end) {
                for (sample in 0 until samplesPerFrame) {
                    val position = frame - sourceStart + sample.toDouble() / samplesPerFrame
                    val from = (position * peaks.size / duration).toInt().coerceIn(0, peaks.lastIndex)
                    val to = kotlin.math.ceil(
                        (position + 1.0 / samplesPerFrame) * peaks.size / duration,
                    ).toInt().coerceIn(from + 1, peaks.size)
                    var peak = 0f
                    for (index in from until to) peak = maxOf(peak, peaks[index].takeIf { it.isFinite() } ?: 0f)
                    val target = (frame - firstFrame) * samplesPerFrame + sample
                    result[target] = maxOf(result[target], (peak * clip.volume).coerceIn(0f, 1f))
                }
            }
        }
        return result.toList()
    }
}
