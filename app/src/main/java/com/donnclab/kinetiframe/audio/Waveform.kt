package com.donnclab.kinetiframe.audio

import kotlin.math.sqrt

/**
 * A rolling set of normalised amplitude peaks, 0f..1f, used to draw a waveform.
 *
 * Deliberately a plain immutable value with no Android dependency: capture reads raw amplitudes
 * from `MediaRecorder.getMaxAmplitude()` and decode reads them from PCM, but neither concern
 * belongs in the maths, so this stays directly unit-testable (see the project's testing mandate)
 * while [com.donnclab.kinetiframe.audio.VoiceRecorder] owns everything stateful and native.
 *
 * WHY no waveform library: the two things this app needs -- a live meter while recording, and a
 * static outline of a finished clip -- are a normalise-and-downsample over numbers the platform
 * already hands us for free. Pulling in a rendering/decoding dependency for that would add weight
 * and a transitive surface for something the framework covers, so peaks are computed here and
 * drawn with Compose's own Canvas.
 */
data class Waveform(val peaks: List<Float> = emptyList()) {

    /**
     * Appends one sampled amplitude, dropping the oldest peak once [maxBars] is reached so the
     * meter scrolls rather than growing without bound during a long take.
     *
     * [raw] is on `MediaRecorder`'s 0..32767 scale. It is square-rooted after normalising because
     * amplitude is linear while hearing is closer to logarithmic -- without it, ordinary speech
     * renders as a nearly flat line and only clipping-loud input looks like anything.
     */
    fun plus(raw: Int, maxBars: Int = DEFAULT_MAX_BARS): Waveform {
        val normalised = (raw.coerceIn(0, MAX_AMPLITUDE).toFloat() / MAX_AMPLITUDE)
        val perceptual = sqrt(normalised).coerceIn(0f, 1f)
        val appended = peaks + perceptual
        return Waveform(if (appended.size <= maxBars) appended else appended.takeLast(maxBars))
    }

    /**
     * Resamples to exactly [target] bars by averaging each source bucket -- used to draw a
     * finished clip at whatever width it occupies. Averaging (rather than picking every Nth peak)
     * keeps a transient from disappearing purely because it fell between two sample points.
     */
    fun downsampled(target: Int): Waveform {
        if (target <= 0 || peaks.isEmpty()) return Waveform(emptyList())
        if (peaks.size <= target) return this
        val bucket = peaks.size.toFloat() / target
        return Waveform(
            List(target) { index ->
                val from = (index * bucket).toInt()
                val to = (((index + 1) * bucket).toInt()).coerceAtMost(peaks.size)
                if (to <= from) {
                    peaks[from.coerceAtMost(peaks.lastIndex)]
                } else {
                    peaks.subList(from, to).average().toFloat()
                }
            },
        )
    }

    val isEmpty: Boolean get() = peaks.isEmpty()

    companion object {
        const val MAX_AMPLITUDE = 32767
        const val DEFAULT_MAX_BARS = 160
        val EMPTY = Waveform(emptyList())
    }
}
