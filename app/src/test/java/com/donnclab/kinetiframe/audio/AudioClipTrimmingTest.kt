package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioClipTrimmingTest {
    @Test fun `widening a trim restores the original audio and waveform`() {
        val trimmed = AudioClipTrimming.shorten(clip, 13, 4, 12)
        val restored = AudioClipTrimming.shorten(trimmed, 10, 12, 12)
        assertEquals(clip.startFrame, restored.startFrame)
        assertEquals(clip.durationFrames, restored.durationFrames)
        assertEquals(clip.trimStartMicros, restored.trimStartMicros)
        assertEquals(clip.waveformPeaks, restored.waveformPeaks)
    }
    private val clip = AudioClipPlacement(
        "c",
        "a",
        0,
        "Voice",
        10,
        12,
        trimStartMicros = 250_000,
        waveformPeaks = List(12) { it / 12f },
    )

    @Test fun `left trim advances source and crops waveform without moving retained sound`() {
        val trimmed = AudioClipTrimming.shorten(clip, 13, 9, 12)
        assertEquals(13, trimmed.startFrame)
        assertEquals(500_000L, trimmed.trimStartMicros)
        assertEquals(clip.waveformPeaks.drop(3), trimmed.waveformPeaks)
        assertEquals(9, trimmed.durationFrames)
    }

    @Test fun `right trim keeps source in point`() {
        val trimmed = AudioClipTrimming.shorten(clip, 10, 6, 12)
        assertEquals(250_000L, trimmed.trimStartMicros)
        assertEquals(clip.waveformPeaks.take(6), trimmed.waveformPeaks)
    }

    @Test fun `invalid trim cannot invent earlier source or empty duration`() {
        val trimmed = AudioClipTrimming.shorten(clip, -50, 0, 12)
        assertEquals(10, trimmed.startFrame)
        assertEquals(1, trimmed.durationFrames)
        assertEquals(clip, AudioClipTrimming.shorten(clip, 11, 2, 0))
    }

    @Test fun `moving a clip keeps its source window`() {
        val moved = clip.copy(startFrame = 100, trackIndex = 2)
        assertEquals(clip.trimStartMicros, moved.trimStartMicros)
        assertEquals(clip.waveformPeaks, moved.waveformPeaks)
    }
}
