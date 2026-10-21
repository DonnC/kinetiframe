package com.donnclab.kinetiframe.data.export

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

/**
 * AAC-encodes one already-mixed PCM buffer ([AudioMixer.MixResult]) and writes it into [muxer].
 * The export pipeline supplies an audio-only spool muxer, started when its format is known.
 * File-backed PCM keeps encoding memory independent of timeline duration.
 */
class AudioEncoder(private val muxer: MediaMuxer) {
    var trackIndex = -1
        private set

    /** Encodes the whole buffer synchronously and returns once every sample has been written. */
    fun encodeAndMux(mix: AudioMixer.MixResult, onMuxerReady: () -> Unit) {
        encode(mix.pcm.size.toLong(), mix.pcm, null, onMuxerReady)
    }

    internal fun encodeAndMux(mix: StreamingAudioMix.PcmFile, onMuxerReady: () -> Unit) {
        mix.file.inputStream().buffered().use { stream ->
            encode(mix.frameCount * AudioMixer.CHANNEL_COUNT, null, stream, onMuxerReady)
        }
    }

    private fun encode(totalSamples: Long, pcm: ShortArray?, stream: java.io.InputStream?, onMuxerReady: () -> Unit) {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            AudioMixer.SAMPLE_RATE_HZ,
            AudioMixer.CHANNEL_COUNT,
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_BPS)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)

        val bufferInfo = MediaCodec.BufferInfo()
        var pcmOffsetSamples = 0L
        val readBuffer = ByteArray(16 * 1024)
        var inputDone = false
        var outputDone = false
        var presentationTimeUs = 0L

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            var lastProgress = System.nanoTime()
            while (!outputDone) {
                check(System.nanoTime() - lastProgress < 30_000_000_000L) { "Audio encoder timed out" }
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        lastProgress = System.nanoTime()
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (pcmOffsetSamples >= totalSamples) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.order(java.nio.ByteOrder.nativeOrder())
                            val samplesToWrite = minOf(
                                inputBuffer.capacity() / 2L,
                                totalSamples - pcmOffsetSamples,
                                readBuffer.size / 2L,
                            ).toInt()
                            val byteBuffer: ByteBuffer = inputBuffer
                            if (pcm != null) {
                                for (i in 0 until samplesToWrite) byteBuffer.putShort(pcm[pcmOffsetSamples.toInt() + i])
                            } else {
                                var read = 0
                                while (read < samplesToWrite * 2) {
                                    val count = requireNotNull(stream).read(readBuffer, read, samplesToWrite * 2 - read)
                                    check(count > 0) { "Mixed audio file is incomplete" }
                                    read += count
                                }
                                byteBuffer.put(readBuffer, 0, read)
                            }
                            codec.queueInputBuffer(inputIndex, 0, samplesToWrite * 2, presentationTimeUs, 0)
                            pcmOffsetSamples += samplesToWrite
                            presentationTimeUs =
                                (pcmOffsetSamples / AudioMixer.CHANNEL_COUNT) * 1_000_000L / AudioMixer.SAMPLE_RATE_HZ
                        }
                    }
                }
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        require(trackIndex == -1) { "Audio output format changed more than once" }
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        onMuxerReady()
                    }

                    outputIndex >= 0 -> {
                        lastProgress = System.nanoTime()
                        val encodedData = codec.getOutputBuffer(outputIndex)
                        if (encodedData != null && bufferInfo.size != 0 && trackIndex != -1 &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                        ) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
    }

    private companion object {
        const val BIT_RATE_BPS = 192_000
        const val TIMEOUT_US = 10_000L
    }
}
