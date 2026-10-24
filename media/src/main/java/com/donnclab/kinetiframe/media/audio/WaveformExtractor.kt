package com.donnclab.kinetiframe.media.audio

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Produces a drawable [Waveform] for an audio file that already exists on disk -- an import, or a
 * recording being re-read rather than captured live.
 *
 * Reuses [AudioMixer.decodeToPcm] rather than standing up a second MediaCodec pipeline. That
 * matters beyond DRY: export renders what that decoder produces, so extracting peaks through the
 * same path guarantees the outline an animator trims against is the audio they will actually hear
 * in the exported video, rather than a second decoder's slightly different interpretation of the
 * same file.
 *
 * Recorded takes do NOT need this -- [com.donnclab.kinetiframe.media.audio.VoiceRecorder] already
 * accumulates peaks from the live meter, so a recording arrives with its waveform for free and
 * decoding it again would be pure waste.
 *
 * Every failure degrades to [Waveform.EMPTY] rather than throwing: a clip whose source cannot be
 * decoded must still place and still play, it just draws as a flat bar.
 */
object WaveformExtractor {

    /**
     * [durationMicros] bounds how much audio is decoded. Decoding is capped at [MAX_DECODE_SECONDS]
     * because this runs only to draw an outline a few hundred pixels wide -- reading a ten-minute
     * import in full would allocate tens of megabytes of PCM to produce the same picture.
     */
    fun fromFile(sourcePath: String, durationMicros: Long, bars: Int = Waveform.DEFAULT_MAX_BARS): Waveform =
        runCatching {
            if (durationMicros <= 0L || bars <= 0) return@runCatching Waveform.EMPTY
            val seconds = min(durationMicros / 1_000_000.0, MAX_DECODE_SECONDS)
            val maxPcmFrames = (seconds * AudioMixer.SAMPLE_RATE_HZ).toInt().coerceAtLeast(1)
            val pcm = AudioMixer().decodeToPcm(sourcePath, trimStartMicros = 0L, maxPcmFrames = maxPcmFrames)
            fromPcm(pcm, bars)
        }.getOrDefault(Waveform.EMPTY)

    /**
     * Buckets interleaved 16-bit PCM into [bars] peaks.
     *
     * Each bucket takes its maximum absolute sample, not its mean: a mean washes transients out
     * into a flat ribbon, and transients are exactly what an animator lines a cut up against. The
     * square root then matches [Waveform.plus]'s perceptual curve so a decoded clip and a live
     * take are drawn on the same scale.
     */
    fun fromPcm(pcm: ShortArray, bars: Int): Waveform {
        if (pcm.isEmpty() || bars <= 0) return Waveform.EMPTY
        val bucketSize = (pcm.size.toDouble() / bars).coerceAtLeast(1.0)
        val peaks = ArrayList<Float>(bars)
        var index = 0
        while (index < bars) {
            val from = (index * bucketSize).toInt().coerceIn(0, pcm.lastIndex)
            val to = (((index + 1) * bucketSize).toInt()).coerceIn(from + 1, pcm.size)
            var peak = 0
            for (i in from until to) {
                val magnitude = abs(pcm[i].toInt())
                if (magnitude > peak) peak = magnitude
            }
            peaks.add(sqrt(peak.toFloat() / Short.MAX_VALUE).coerceIn(0f, 1f))
            index++
        }
        return Waveform(peaks)
    }

    private const val MAX_DECODE_SECONDS = 120.0
}
