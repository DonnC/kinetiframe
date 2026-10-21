package com.donnclab.kinetiframe.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Main-thread owned. prepareAsync avoids blocking Compose on media decoding or file setup. */
class AndroidPreviewAudioPlayer : PreviewAudioPlayer {
    private var player: MediaPlayer? = null
    private var failure: IOException? = null
    override suspend fun prepare(path: String): Int {
        release()
        require(File(path).isFile) { "Audio file is missing" }
        val native = MediaPlayer()
        player = native
        try {
            native.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            )
            native.setDataSource(path)
            return suspendCancellableCoroutine { continuation ->
                // Listener callbacks only bridge the native API into coroutine cancellation.
                native.setOnPreparedListener { if (continuation.isActive) continuation.resume(it.duration) }
                native.setOnErrorListener { _, what, extra ->
                    failure = IOException("Media error $what/$extra")
                    if (continuation.isActive) continuation.resumeWithException(requireNotNull(failure))
                    true
                }
                continuation.invokeOnCancellation { if (player === native) release() }
                native.prepareAsync()
            }
        } catch (error: Exception) {
            if (player === native) release()
            throw error
        }
    }
    private fun prepared(): MediaPlayer {
        failure?.let { throw it }
        return checkNotNull(player)
    }
    override fun play() = prepared().start()
    override fun pause() = prepared().pause()
    override fun seek(positionMs: Int) = prepared().seekTo(positionMs.toLong(), MediaPlayer.SEEK_CLOSEST)
    override val positionMs: Int get() = prepared().currentPosition
    override val playing: Boolean get() = prepared().isPlaying
    override fun release() {
        val old = player
        player = null
        failure = null
        old?.setOnPreparedListener(null)
        old?.setOnErrorListener(null)
        old?.release()
    }
}
