package com.donnclab.kinetiframe.media.audio

import kotlin.math.roundToInt

/**
 * The in/out points an animator has chosen within a source recording, in microseconds.
 *
 * Pure and Android-free so the clamping rules -- the part that actually goes wrong -- can be
 * exercised directly in a JVM test rather than only by dragging handles on a device. Every
 * mutation returns a new, already-valid selection: it is impossible to hold an inverted or
 * zero-length range, which is what the placement model refuses anyway.
 *
 * Microseconds, not frames, because a trim is a fact about the audio file and must survive the
 * project's frame rate changing underneath it -- the same reason keyframes are stored as time.
 * [toDurationFrames] converts only at the moment of placement.
 */
data class AudioTrimSelection(
    val sourceDurationMicros: Long,
    val inMicros: Long = 0L,
    val outMicros: Long = sourceDurationMicros,
) {
    init {
        require(sourceDurationMicros > 0L) { "A trim needs a source with a positive duration" }
    }

    val selectedDurationMicros: Long get() = (outMicros - inMicros).coerceAtLeast(MIN_SELECTION_MICROS)

    /** Fraction of the whole source each edge sits at, for positioning handles over a waveform
     *  drawn at any width. */
    val inFraction: Float get() = (inMicros.toFloat() / sourceDurationMicros).coerceIn(0f, 1f)
    val outFraction: Float get() = (outMicros.toFloat() / sourceDurationMicros).coerceIn(0f, 1f)

    /** Moves the in-point, never past the out-point (less a minimum selection) and never before
     *  the start of the file. */
    fun withIn(micros: Long): AudioTrimSelection =
        copy(inMicros = micros.coerceIn(0L, (outMicros - MIN_SELECTION_MICROS).coerceAtLeast(0L)))

    /** Moves the out-point, never before the in-point (plus a minimum selection) and never past
     *  the end of the file. */
    fun withOut(micros: Long): AudioTrimSelection = copy(
        outMicros = micros.coerceIn(
            (inMicros + MIN_SELECTION_MICROS).coerceAtMost(sourceDurationMicros),
            sourceDurationMicros,
        ),
    )

    /** Drag helper: [fraction] is a 0f..1f position along the waveform. */
    fun withInFraction(fraction: Float): AudioTrimSelection =
        withIn((fraction.coerceIn(0f, 1f) * sourceDurationMicros).toLong())

    fun withOutFraction(fraction: Float): AudioTrimSelection =
        withOut((fraction.coerceIn(0f, 1f) * sourceDurationMicros).toLong())

    /**
     * How many animation frames the selection occupies at [fps].
     *
     * Rounds to nearest rather than truncating: a selection is far more often meant to reach the
     * beat the animator trimmed to than to stop just short of it, and a floor would silently clip
     * the tail of every take that did not land on an exact frame boundary. Always at least one
     * frame, since a zero-length placement is invalid.
     */
    fun toDurationFrames(fps: Int): Int {
        if (fps <= 0) return 1
        return (selectedDurationMicros * fps / 1_000_000.0).roundToInt().coerceAtLeast(1)
    }

    /** The portion of a full-source waveform this selection covers, for drawing the trimmed clip
     *  without decoding again. */
    fun slice(waveform: Waveform): Waveform {
        if (waveform.isEmpty) return Waveform.EMPTY
        val from = (waveform.peaks.size * inFraction).toInt().coerceIn(0, waveform.peaks.size)
        val to = (waveform.peaks.size * outFraction).toInt().coerceIn(from, waveform.peaks.size)
        if (to <= from) return Waveform.EMPTY
        return Waveform(waveform.peaks.subList(from, to).toList())
    }

    companion object {
        /** 50ms -- below this a "clip" is a click, not a sound, and the handles become impossible
         *  to separate by touch. */
        const val MIN_SELECTION_MICROS = 50_000L
    }
}
