package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AudioEffectsTest {
    @Test fun `crossfade moves next clip earlier and midpoint power sums to one`() {
        val (a, b) = AudioCrossfade.apply(
            AudioClipPlacement("a", "s", 0, "A", 0, 24),
            AudioClipPlacement("b", "s", 0, "B", 24, 24),
            12,
        )
        assertEquals(12, b.startFrame)
        val outgoing = AudioEnvelope.gain(a, 1_500_000, 12)
        val incoming = AudioEnvelope.gain(b, 500_000, 12)
        assertEquals(1f, outgoing * outgoing + incoming * incoming, .0001f)
    }

    @Test fun `voice styles are bounded and chunk boundaries do not change the signal`() {
        VoiceEffect.entries.forEach { effect ->
            val source = ShortArray(8820) { (kotlin.math.sin(it / 30.0) * 30000).toInt().toShort() }
            val whole = source.copyOf()
            VoiceFilter(effect).process(whole)
            val a = source.copyOfRange(0, 4000)
            val b = source.copyOfRange(4000, source.size)
            val filter = VoiceFilter(effect)
            filter.process(a)
            filter.process(b)
            assertArrayEquals(whole, a + b, effect.name)
            if (effect ==
                VoiceEffect.ORIGINAL
            ) {
                assertArrayEquals(source, whole)
            } else {
                assertFalse(source.contentEquals(whole))
            }
        }
    }
}
