package com.donnclab.kinetiframe.audio.data

import com.donnclab.kinetiframe.audio.VoiceFilter
import com.donnclab.kinetiframe.core.document.VoiceEffect
import com.donnclab.kinetiframe.export.data.AudioMixer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Processing happens off Main. Preview/scrubbing and export subsequently read the same WAV.
 * Disk-backed, one-second decode windows avoid retaining an entire recording in heap. */
internal class VoiceEffectProxy(private val decode: (String, Long, Int) -> ShortArray = AudioMixer()::decodeToPcm) {
    suspend fun create(source: File, durationMicros: Long, effect: VoiceEffect, output: File) {
        require(source.isFile && durationMicros > 0 && effect != VoiceEffect.ORIGINAL)
        require(durationMicros <= (Int.MAX_VALUE.toLong() - 36) * 1_000_000 / (44100 * 4)) {
            "Audio is too long for a voice-effect WAV"
        }
        val frames = durationMicros * 44100 / 1_000_000
        val dataBytes = frames * 4
        require(dataBytes in 1..(Int.MAX_VALUE.toLong() - 36)) { "Audio is too long for a voice-effect WAV" }
        output.parentFile?.mkdirs()
        require(output.parentFile?.usableSpace ?: 0L > dataBytes + 16L * 1024 * 1024) {
            "Not enough storage for this voice effect"
        }
        val filter = VoiceFilter(effect)
        val bytes = ByteBuffer.allocate(44100 * 4).order(ByteOrder.LITTLE_ENDIAN)
        try {
            RandomAccessFile(output, "rw").use { file ->
                file.setLength(0)
                file.write(header(dataBytes.toInt()))
                var start = 0L
                while (start < frames) {
                    currentCoroutineContext().ensureActive()
                    val count = minOf(44100L, frames - start).toInt()
                    val pcm = decode(source.absolutePath, start * 1_000_000 / 44100, count)
                    // A video's picture can outlast its sound track. Preserve that silent tail.
                    check(start > 0 || pcm.isNotEmpty()) { "Could not decode this audio source" }
                    filter.process(pcm)
                    bytes.clear()
                    for (index in 0 until count * 2) bytes.putShort(pcm.getOrElse(index) { 0 })
                    file.write(bytes.array(), 0, count * 4)
                    start += count
                }
            }
        } catch (failure: Throwable) {
            output.delete()
            throw failure
        }
    }
    private fun header(length: Int): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt(length + 36)
        put("WAVEfmt ".toByteArray())
        putInt(16)
        putShort(1)
        putShort(2)
        putInt(44100)
        putInt(44100 * 4)
        putShort(4)
        putShort(16)
        put("data".toByteArray())
        putInt(length)
    }.array()
}
