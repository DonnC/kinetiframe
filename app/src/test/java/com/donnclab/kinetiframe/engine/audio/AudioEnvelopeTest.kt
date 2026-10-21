package com.donnclab.kinetiframe.engine.audio

import com.donnclab.kinetiframe.domain.manager.AudioManager
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.ui.TimelineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioEnvelopeTest {
    private val clip =
        AudioClipPlacement("a", "asset", 0, "Sound", 0, 48, volume = .8f, fadeInFrames = 12, fadeOutFrames = 12)

    @Test fun `fade gains match source clock and clip volume`() {
        assertEquals(0f, AudioEnvelope.gain(clip, 0, 24))
        assertEquals(.4f, AudioEnvelope.gain(clip, 250_000, 24), .001f)
        assertEquals(.8f, AudioEnvelope.gain(clip, 1_000_000, 24), .001f)
        assertEquals(.4f, AudioEnvelope.gain(clip, 1_750_000, 24), .001f)
        assertEquals(0f, AudioEnvelope.gain(clip, 2_000_000, 24))
        assertEquals(0f, AudioEnvelope.gain(clip, -1, 24))
    }

    @Test fun `splitting preserves fade gain on both sides without restarting envelope`() {
        var state =
            TimelineState(
                project = com.donnclab.kinetiframe.engine.model.ProjectDocument(
                    fps = 24,
                    audioClips = mapOf("a" to clip),
                ),
            )
        AudioManager({ state }, { state = it(state) }).split("a", 6)
        val left = state.project.audioClips.getValue("a")
        val right = state.project.audioClips.values.single { it.clipId != "a" }
        assertEquals(AudioEnvelope.gain(clip, 125_000, 24), AudioEnvelope.gain(left, 125_000, 24))
        assertEquals(AudioEnvelope.gain(clip, 375_000, 24), AudioEnvelope.gain(right, 125_000, 24))
        assertEquals(AudioEnvelope.gain(clip, 1_750_000, 24), AudioEnvelope.gain(right, 1_500_000, 24))
    }

    @Test fun `no fades preserves existing constant volume`() {
        assertEquals(.8f, AudioEnvelope.gain(clip.copy(fadeInFrames = 0, fadeOutFrames = 0), 300_000, 24))
    }
}
