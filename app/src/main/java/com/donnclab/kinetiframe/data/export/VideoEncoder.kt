package com.donnclab.kinetiframe.data.export

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer

/**
 * Drives the H.264 hardware encoder for one export run: one frame in, zero or more encoded
 * samples out, muxed by whatever [MediaMuxer] the caller is coordinating with its audio track.
 *
 * Two correctness requirements this class exists to guarantee:
 *  - End-of-stream draining: without [signalEndOfStreamAndDrain], the last few frames queued into
 *    the encoder's input surface can still be in flight when the loop ends, and simply calling
 *    `codec.stop()` drops them silently -- the exported clip would be short a beat or two.
 *  - An opaque backdrop: stage art can have transparent regions (an unset background, gaps
 *    between drawn strokes), but H.264 has no alpha channel -- undrawn pixels would decode as
 *    whatever garbage the GPU driver leaves in the surface, so every frame paints solid black
 *    first, in [encodeFrame].
 */
class VideoEncoder(private val muxer: MediaMuxer, width: Int, height: Int, fps: Int) {
    private val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, ExportBitrate.bitsPerSecond(width, height, fps))
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }
    private val inputSurface = codec.createInputSurface()
    private val renderer = try {
        EncoderInputSurface(inputSurface, width, height)
    } catch (error: Exception) {
        inputSurface.release()
        codec.release()
        throw error
    }
    private val bufferInfo = MediaCodec.BufferInfo()

    var trackIndex = -1
        private set

    fun start() {
        codec.start()
    }

    /** Paints [bitmap] onto the encoder's input surface over an opaque black backdrop, then locks
     *  in [presentationTimeUs] and drains whatever the encoder is ready to emit. */
    fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long, onMuxerReady: () -> Unit) {
        require(!bitmap.isRecycled) { "Export frame is recycled" }
        renderer.draw(bitmap, presentationTimeUs)
        drain(endOfStream = false, onMuxerReady)
    }

    /** Signals end-of-stream to the encoder and drains every remaining buffer, so frames still in
     *  flight are not silently dropped. Must be called exactly once, after the last [encodeFrame]. */
    fun signalEndOfStreamAndDrain(lastPresentationTimeUs: Long, onMuxerReady: () -> Unit) {
        codec.signalEndOfInputStream()
        drain(endOfStream = true, onMuxerReady)
    }

    fun release() {
        runCatching { renderer.release() }
        runCatching { codec.stop() }
        codec.release()
        inputSurface.release()
    }

    private fun drain(endOfStream: Boolean, onMuxerReady: () -> Unit) {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (true) {
            check(System.nanoTime() < deadline) { "Video encoder timed out while draining" }
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (endOfStream) continue else return
                }

                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    require(trackIndex == -1) { "Video output format changed more than once" }
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    onMuxerReady()
                }

                outputBufferIndex >= 0 -> {
                    val encodedData = codec.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size != 0 && trackIndex != -1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
