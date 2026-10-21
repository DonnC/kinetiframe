package com.donnclab.kinetiframe.domain.manager

import android.media.MediaPlayer
import com.donnclab.kinetiframe.engine.audio.AudioTrimSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Auditions the selected part of a take while the animator trims it.
 *
 * Owns its `MediaPlayer` for the same reason [VoiceRecorder] owns its recorder: a native player
 * driven from composable local state can be stranded by a recomposition or an unexpected
 * dismissal, and this one is created and destroyed every time a trim screen opens.
 *
 * Playback is bounded by the selection rather than by the file: the point of previewing a trim is
 * to hear exactly what the clip will sound like once placed, so it starts at the in-point and
 * stops itself at the out-point instead of running on into audio the animator has just excluded.
 */
class TrimPreviewPlayer(
    private val scope: CoroutineScope,
    private val createPlayer: () -> MediaPlayer = { MediaPlayer() },
) {
    data class State(val isPlaying: Boolean = false, val positionMicros: Long = 0L)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var tickJob: Job? = null

    /** Starts (or restarts) preview of [selection] within [sourcePath]. Any failure leaves the
     *  screen usable and simply silent -- a take that cannot be auditioned can still be trimmed
     *  and placed by its waveform. */
    fun play(sourcePath: String, selection: AudioTrimSelection) {
        stop()
        val prepared = runCatching {
            createPlayer().apply {
                setDataSource(sourcePath)
                prepare()
                seekTo(selection.inMicros / 1000L, MediaPlayer.SEEK_CLOSEST)
                start()
            }
        }.getOrNull() ?: return
        player = prepared
        _state.value = State(isPlaying = true, positionMicros = selection.inMicros)
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val current = player ?: break
                val positionMicros = runCatching { current.currentPosition * 1_000L }.getOrDefault(0L)
                if (positionMicros >= selection.outMicros) {
                    stop()
                    break
                }
                _state.value = State(isPlaying = true, positionMicros = positionMicros)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        player?.let { current ->
            runCatching { if (current.isPlaying) current.stop() }
            runCatching { current.release() }
        }
        player = null
        _state.value = State()
    }

    /** Must be called when the trim screen leaves composition. */
    fun release() = stop()

    private companion object {
        const val TICK_MS = 40L
    }
}
