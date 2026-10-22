package com.donnclab.kinetiframe.export.data

import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.ProjectAsset
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

/** One-second mix windows keep export RAM independent of timeline duration or track count. */
internal class StreamingAudioMix(private val decodePcm: (String, Long, Int) -> ShortArray = AudioMixer()::decodeToPcm) {
    data class PcmFile(val file: File, val frameCount: Long)

    fun mix(
        clips: Collection<AudioClipPlacement>,
        assets: Map<String, ProjectAsset>,
        fps: Int,
        totalFrames: Int,
        output: File,
    ): PcmFile? {
        if (clips.isEmpty()) return null
        val sampleRate = AudioMixer.SAMPLE_RATE_HZ
        val channels = AudioMixer.CHANNEL_COUNT
        val length = (totalFrames.toDouble() * sampleRate / fps).roundToLong().coerceAtLeast(1)
        val master = FloatArray(sampleRate * channels)
        val bytes = ByteBuffer.allocate(sampleRate * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        output.outputStream().buffered().use { stream ->
            var windowStart = 0L
            while (windowStart < length) {
                java.util.Arrays.fill(master, 0f)
                val count = minOf(sampleRate.toLong(), length - windowStart).toInt()
                clips.forEach { clip ->
                    val start = (clip.startFrame.toDouble() * sampleRate / fps).roundToLong()
                    val end = start + (clip.durationFrames.toDouble() * sampleRate / fps).roundToLong()
                    val from = maxOf(start, windowStart)
                    val to = minOf(end, windowStart + count)
                    if (from < to) {
                        val path =
                            requireNotNull(assets[clip.assetId]?.sourcePath) {
                                "Audio source is missing: ${clip.label}"
                            }
                        require(File(path).isFile) { "Audio source is missing: ${clip.label}" }
                        val trim = clip.trimStartMicros + (from - start) * 1_000_000L / sampleRate
                        val pcm = decodePcm(path, trim, (to - from).toInt())
                        val destination = (from - windowStart).toInt() * channels
                        for (index in 0 until minOf(pcm.size, count * channels - destination)) {
                            val relativeMicros = (from - start + index / channels) * 1_000_000L / sampleRate
                            master[destination + index] += pcm[index].toFloat() / Short.MAX_VALUE *
                                com.donnclab.kinetiframe.audio.AudioEnvelope.gain(clip, relativeMicros, fps)
                        }
                    }
                }
                bytes.clear()
                for (index in 0 until count * channels) {
                    bytes.putShort((master[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
                }
                stream.write(bytes.array(), 0, bytes.position())
                windowStart += count
            }
        }
        return PcmFile(output, length)
    }
}
