package com.donnclab.kinetiframe.engine.audio

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.tanh

@Serializable
enum class VoiceEffect { ORIGINAL, WARM, RADIO, ROBOT }

/** Stateful per-channel filters retain continuity across bounded PCM windows. No timing changes. */
class VoiceFilter(private val effect: VoiceEffect, private val rate: Int = 44100, private val channels: Int = 2) {
    private val low = DoubleArray(channels)
    private val bass = DoubleArray(channels)
    private var sampleFrame = 0L
    private val alpha = 1 - exp(-2 * PI * (if (effect == VoiceEffect.RADIO) 2800.0 else 3600.0) / rate)
    private val bassAlpha = 1 - exp(-2 * PI * 300 / rate)
    fun process(pcm: ShortArray) {
        for (frame in 0 until pcm.size / channels) {
            for (channel in 0 until channels) {
                val index = frame * channels + channel
                val sample = pcm[index].toDouble() / 32768
                low[channel] += alpha * (sample - low[channel])
                bass[channel] += bassAlpha * (sample - bass[channel])
                val value = when (effect) {
                    VoiceEffect.ORIGINAL -> sample
                    VoiceEffect.WARM -> low[channel] * .9 + bass[channel] * .1
                    VoiceEffect.RADIO -> tanh((low[channel] - bass[channel]) * 2.0) * .65
                    VoiceEffect.ROBOT -> sample * cos(2 * PI * 45 * sampleFrame / rate)
                }
                pcm[index] = (value.coerceIn(-1.0, .999969) * 32768).roundToInt().toShort()
            }
            sampleFrame++
        }
    }
}
