package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectAsset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StreamingAudioFadeTest {
    @TempDir lateinit var root: File

    @Test fun `sample accurate fades cross streaming window boundaries without resetting`() {
        val source = File(root, "source").apply { writeBytes(byteArrayOf(1)) }
        val output = File(root, "mixed.pcm")
        val windows = mutableListOf<Pair<Long, Int>>()
        val mixer = StreamingAudioMix { _, trim, count ->
            windows += trim to count
            ShortArray(count * AudioMixer.CHANNEL_COUNT) { Short.MAX_VALUE }
        }
        val clip = AudioClipPlacement(
            "a", "source", 0, "Voice", 0, 48,
            volume = .8f, fadeInFrames = 12, fadeOutFrames = 12,
        )
        val asset = ProjectAsset("source", AssetKind.AUDIO, source.absolutePath, 0, 0)
        val result = requireNotNull(mixer.mix(listOf(clip), mapOf("source" to asset), 24, 48, output))
        val pcm = ByteBuffer.wrap(output.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        fun gainAt(frame: Int) = pcm.get(frame * 2).toFloat() / Short.MAX_VALUE
        assertEquals(88_200L, result.frameCount)
        assertEquals(2, windows.size)
        assertEquals(0f, gainAt(0), .0001f)
        assertEquals(.4f, gainAt(11_025), .0001f)
        assertEquals(.8f, gainAt(44_100), .0001f)
        assertEquals(.4f, gainAt(77_175), .0001f)
        assertEquals(0f, gainAt(88_199), .0001f)
        assertEquals(pcm.get(22_050), pcm.get(22_051)) // Both stereo channels share the same envelope.
    }
}
