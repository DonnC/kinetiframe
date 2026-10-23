package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.AudioClipSourceWindow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimelineAudioSummaryTest {
    private fun clip(id: String = "voice", lane: Int = 0) = AudioClipPlacement(
        id,
        "source",
        lane,
        "Voice",
        2,
        2,
        waveformPeaks = listOf(0f, 1f, .2f, .4f),
    )

    @Test fun `short transients are retained and aligned to frames`() {
        assertEquals(listOf(0f, 0f, 1f, .4f, 0f), TimelineAudioSummary.peaksPerFrame(listOf(clip()), 5))
    }

    @Test fun `mix respects lane mute and individual volume`() {
        assertEquals(
            listOf(.5f, .2f),
            TimelineAudioSummary.window(listOf(clip().copy(volume = .5f), clip("music", 1)), 2, 2, setOf(1)),
        )
    }

    @Test fun `trim source maps offset not whole source into shortened clip`() {
        val trimmed = clip().copy(
            trimOffsetFrames = 2,
            trimSource = AudioClipSourceWindow(0, 4, listOf(.1f, .2f, .7f, .9f)),
        )
        assertEquals(listOf(.7f, .9f, 0f), TimelineAudioSummary.window(listOf(trimmed), 2, 3))
    }

    @Test fun `empty data and zero window are safe`() {
        assertEquals(emptyList<Float>(), TimelineAudioSummary.window(listOf(clip()), 0, 0))
        assertEquals(
            listOf(0f, 0f),
            TimelineAudioSummary.window(listOf(clip().copy(waveformPeaks = emptyList())), 2, 2),
        )
    }

    @Test fun `subframe samples retain waveform detail without allocating the entire project`() {
        val source = clip().copy(durationFrames = 1, waveformPeaks = listOf(0f, .3f, 1f, .1f))
        assertEquals(listOf(0f, .3f, 1f, .1f), TimelineAudioSummary.window(listOf(source), 2, 1, samplesPerFrame = 4))
    }
}
