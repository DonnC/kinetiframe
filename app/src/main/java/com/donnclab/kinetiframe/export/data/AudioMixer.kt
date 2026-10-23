package com.donnclab.kinetiframe.export.data

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectAsset
import kotlin.math.roundToInt

/**
 * Mixes every [AudioClipPlacement] on every track into one master PCM stream, upfront and fully
 * synchronously, before the video encoder is started. Decoding and mixing audio is fast and has
 * no real-time constraint, so doing it first means the audio track's [MediaFormat], and therefore
 * both muxer track indices, are known before the first video frame is drawn. Nothing has to be
 * buffered while waiting on the other track.
 *
 * Overlapping clips are summed and resampled linearly. This path has no unit tests yet; clipping
 * or drift on long mixes is a known risk.
 *
 * Silent by design if a project has no audio clips at all -- [mix] then returns null and
 * [ProjectExporter] simply skips adding an audio track.
 */
class AudioMixer {
    /** One fully-mixed, 16-bit signed PCM master buffer at [SAMPLE_RATE_HZ]/[CHANNEL_COUNT], plus
     *  its length in frames (a "frame" here is one sample per channel, the PCM sense of the word,
     *  not an animation frame). */
    data class MixResult(val pcm: ShortArray, val frameCount: Int)

    fun mix(
        clips: Collection<AudioClipPlacement>,
        assets: Map<String, ProjectAsset>,
        projectFps: Int,
        totalAnimationFrames: Int,
    ): MixResult? {
        if (clips.isEmpty()) return null
        val totalSeconds = totalAnimationFrames.toDouble() / projectFps.toDouble()
        val totalPcmFrames = (totalSeconds * SAMPLE_RATE_HZ).roundToInt().coerceAtLeast(1)
        val master = FloatArray(totalPcmFrames * CHANNEL_COUNT)
        var mixedAny = false
        for (clip in clips) {
            val asset = assets[clip.assetId] ?: continue
            val sourcePath = asset.sourcePath ?: continue
            val startPcmFrame = ((clip.startFrame.toDouble() / projectFps.toDouble()) * SAMPLE_RATE_HZ).roundToInt()
            val maxPcmFrames = ((clip.durationFrames.toDouble() / projectFps.toDouble()) * SAMPLE_RATE_HZ).roundToInt()
            val decoded = runCatching {
                decodeToPcm(sourcePath, clip.trimStartMicros, maxPcmFrames)
            }.getOrNull() ?: continue
            mixInto(master, decoded, startPcmFrame, clip, projectFps)
            mixedAny = true
        }
        if (!mixedAny) return null
        val pcm = ShortArray(master.size)
        for (i in master.indices) {
            pcm[i] = (master[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return MixResult(pcm, totalPcmFrames)
    }

    private fun mixInto(
        master: FloatArray,
        clipPcm: ShortArray,
        startPcmFrame: Int,
        clip: AudioClipPlacement,
        fps: Int,
    ) {
        val clipFrameCount = clipPcm.size / CHANNEL_COUNT
        for (frame in 0 until clipFrameCount) {
            val volume = com.donnclab.kinetiframe.audio.AudioEnvelope.gain(
                clip,
                frame.toLong() * 1_000_000L / SAMPLE_RATE_HZ,
                fps,
            )
            val destFrame = startPcmFrame + frame
            if (destFrame < 0 || destFrame >= master.size / CHANNEL_COUNT) continue
            for (channel in 0 until CHANNEL_COUNT) {
                val destIndex = destFrame * CHANNEL_COUNT + channel
                val sampleIndex = frame * CHANNEL_COUNT + channel
                master[destIndex] += (clipPcm[sampleIndex].toFloat() / Short.MAX_VALUE) * volume
            }
        }
    }

    /**
     * Decodes [sourcePath] starting at [trimStartMicros], up to [maxPcmFrames] output frames,
     * resampled with linear interpolation and channel-mapped to [SAMPLE_RATE_HZ]/[CHANNEL_COUNT].
     *
     * Internal so [com.donnclab.kinetiframe.audio.WaveformExtractor] decodes through the
     * same path that export renders with, rather than a second MediaCodec pipeline.
     */
    internal fun decodeToPcm(sourcePath: String, trimStartMicros: Long, maxPcmFrames: Int): ShortArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(sourcePath)
        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run {
            extractor.release()
            throw IllegalStateException("No audio track in $sourcePath")
        }
        extractor.selectTrack(trackIndex)
        extractor.seekTo(trimStartMicros, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        var sourceSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var sourceChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        // Primitive bytes avoid boxing every decoded sample. Export asks for one-second windows;
        // longer waveform callers have an explicit cap instead of risking process-wide OOM.
        val sourceBytes = java.io.ByteArrayOutputStream()
        val decodeBudget = minOf(16L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 12)
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        try {
            var lastProgress = System.nanoTime()
            while (!sawOutputEos) {
                check(System.nanoTime() - lastProgress < 30_000_000_000L) { "Audio decoder timed out" }
                if (!sawInputEos) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        lastProgress = System.nanoTime()
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val sampleSize = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    sourceSampleRate = decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    sourceChannels = decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (decoder.outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        require(
                            decoder.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) ==
                                android.media.AudioFormat.ENCODING_PCM_16BIT,
                        ) { "Unsupported decoded PCM format" }
                    }
                }
                if (outputIndex >= 0) {
                    lastProgress = System.nanoTime()
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val bytesPerFrame = sourceChannels * 2
                        val skip =
                            (
                                (
                                    (trimStartMicros - bufferInfo.presentationTimeUs).coerceAtLeast(0L) *
                                        sourceSampleRate +
                                        999_999L
                                    ) /
                                    1_000_000L
                                )
                                .coerceAtMost(bufferInfo.size.toLong() / bytesPerFrame).toInt() * bytesPerFrame
                        val remaining = bufferInfo.size - skip
                        check(sourceBytes.size().toLong() + remaining <= decodeBudget) {
                            "Audio decode exceeds memory budget"
                        }
                        outputBuffer.position(bufferInfo.offset + skip)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val samples = ByteArray(remaining)
                        outputBuffer.get(samples)
                        sourceBytes.write(samples)
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEos = true
                }
                val sourceFrameCount = sourceBytes.size() / (sourceChannels.coerceAtLeast(1) * 2)
                if (maxPcmFrames > 0 &&
                    sourceFrameCount.toLong() * SAMPLE_RATE_HZ / sourceSampleRate.coerceAtLeast(1) >= maxPcmFrames
                ) {
                    break
                }
            }
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
            extractor.release()
        }

        val bytes = sourceBytes.toByteArray()
        val shorts = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(shorts.remaining())
        shorts.get(samples)
        return resample(samples, sourceSampleRate, sourceChannels, maxPcmFrames)
    }

    /** Linear-interpolation resample + downmix/upmix to [CHANNEL_COUNT] stereo output. */
    private fun resample(
        source: ShortArray,
        sourceSampleRate: Int,
        sourceChannels: Int,
        maxPcmFrames: Int,
    ): ShortArray {
        if (source.isEmpty() || sourceChannels <= 0) return ShortArray(0)
        val sourceFrameCount = source.size / sourceChannels
        val ratio = SAMPLE_RATE_HZ.toDouble() / sourceSampleRate.toDouble()
        var outputFrameCount = (sourceFrameCount * ratio).roundToInt()
        if (maxPcmFrames > 0) outputFrameCount = outputFrameCount.coerceAtMost(maxPcmFrames)
        val output = ShortArray(outputFrameCount * CHANNEL_COUNT)
        for (outFrame in 0 until outputFrameCount) {
            val sourcePosition = outFrame / ratio
            val sourceFrameLow = sourcePosition.toInt().coerceIn(0, sourceFrameCount - 1)
            val sourceFrameHigh = (sourceFrameLow + 1).coerceAtMost(sourceFrameCount - 1)
            val fraction = (sourcePosition - sourceFrameLow).toFloat()
            for (outChannel in 0 until CHANNEL_COUNT) {
                val sourceChannel = if (sourceChannels == 1) 0 else outChannel.coerceAtMost(sourceChannels - 1)
                val low = source[sourceFrameLow * sourceChannels + sourceChannel]
                val high = source[sourceFrameHigh * sourceChannels + sourceChannel]
                output[outFrame * CHANNEL_COUNT + outChannel] = (low + (high - low) * fraction).toInt().toShort()
            }
        }
        return output
    }

    companion object {
        const val SAMPLE_RATE_HZ = 44_100
        const val CHANNEL_COUNT = 2
        private const val TIMEOUT_US = 10_000L
    }
}
