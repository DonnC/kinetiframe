package com.donnclab.kinetiframe.audio.data

import com.donnclab.kinetiframe.core.document.VoiceEffect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceEffectProxyTest {
    @TempDir lateinit var root: File

    @Test fun `processed WAV is bounded per decode and retains exact source duration`() = runBlocking {
        val source = File(root, "original.mp4").apply { writeText("owned source") }
        val output = File(root, "processed.wav")
        val requests = mutableListOf<Pair<Long, Int>>()
        VoiceEffectProxy { _, from, count ->
            requests += from to count
            ShortArray(count * 2) { 1000 }
        }
            .create(source, 2_500_000L, VoiceEffect.ROBOT, output)
        assertEquals(listOf(0L to 44100, 1_000_000L to 44100, 2_000_000L to 22050), requests)
        assertEquals(44L + 44100 * 4 * 2.5, output.length().toDouble())
        val header = ByteBuffer.wrap(output.readBytes(), 0, 44).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(441000, header.getInt(40))
        assertEquals("owned source", source.readText())
    }

    @Test fun `decode failure removes incomplete output and leaves original untouched`() = runBlocking {
        val source = File(root, "original.wav").apply { writeText("original") }
        val output = File(root, "processed.wav")
        val result =
            runCatching {
                VoiceEffectProxy { _, _, _ ->
                    error("decoder failed")
                }.create(source, 1_000_000, VoiceEffect.WARM, output)
            }
        assertTrue(result.isFailure)
        assertFalse(output.exists())
        assertEquals("original", source.readText())
    }

    @Test fun `video silent tail stays silent without shortening picture duration`() = runBlocking {
        val source = File(root, "original.mp4").apply { writeText("container") }
        val output = File(root, "processed.wav")
        VoiceEffectProxy { _, from, count -> if (from == 0L) ShortArray(count * 2) { 100 } else shortArrayOf() }
            .create(source, 2_000_000, VoiceEffect.ROBOT, output)
        assertEquals(44L + 88200 * 4, output.length())
        assertTrue(output.readBytes().takeLast(44100 * 4).all { it == 0.toByte() })
    }
}
