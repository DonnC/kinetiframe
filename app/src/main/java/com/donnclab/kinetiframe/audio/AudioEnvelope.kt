package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.core.document.AudioClipPlacement

/** One envelope for audition and sample-accurate export. Overlapping fades multiply. */
object AudioEnvelope {
    fun gain(clip: AudioClipPlacement, relativeMicros: Long, fps: Int): Float {
        require(fps > 0)
        if (relativeMicros < 0 || relativeMicros >= clip.durationFrames * 1_000_000L / fps) return 0f
        val span = clip.envelopeDurationFrames.takeIf { it > 0 } ?: clip.durationFrames
        val duration = span * 1_000_000L / fps
        val position = relativeMicros + clip.envelopeOffsetFrames * 1_000_000L / fps
        val fadeIn = minOf(clip.fadeInFrames, span) * 1_000_000L / fps
        val fadeOut = minOf(clip.fadeOutFrames, span) * 1_000_000L / fps
        val incoming = if (fadeIn == 0L) 1f else (position.toDouble() / fadeIn).toFloat().coerceIn(0f, 1f)
        val outgoing = if (fadeOut ==
            0L
        ) {
            1f
        } else {
            ((duration - position).toDouble() / fadeOut).toFloat().coerceIn(0f, 1f)
        }
        fun curve(gain: Float) =
            if (clip.equalPowerFades) kotlin.math.sin(gain * kotlin.math.PI / 2).toFloat() else gain
        return clip.volume * curve(incoming) * curve(outgoing)
    }
}
