package com.donnclab.kinetiframe.presentation.media.audio

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Native playback is isolated so lifecycle, errors and transport are testable without a device. */
interface PreviewAudioPlayer {
    suspend fun prepare(path: String): Int
    fun play()
    fun pause()
    fun seek(positionMs: Int)
    val positionMs: Int
    val playing: Boolean
    fun release()
}
data class AudioPreviewState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val playing: Boolean = false,
    val durationMs: Int = 0,
    val positionMs: Int = 0,
    val error: String? = null,
) : ViewState
sealed interface AudioPreviewIntent : ViewIntent {
    data class Load(val path: String) : AudioPreviewIntent
    data object Toggle : AudioPreviewIntent
    data class Seek(val positionMs: Int) : AudioPreviewIntent
    data object Stop : AudioPreviewIntent
}
sealed interface AudioPreviewEffect : ViewEffect

class AudioPreviewViewModel(private val player: PreviewAudioPlayer) :
    BaseViewModel<AudioPreviewState, AudioPreviewIntent, AudioPreviewEffect>(AudioPreviewState()) {
    private var loadJob: Job? = null
    private var positionJob: Job? = null
    override fun handleIntent(intent: AudioPreviewIntent) {
        when (intent) {
            is AudioPreviewIntent.Load -> {
                stop()
                updateState { AudioPreviewState(loading = true) }
                loadJob = viewModelScope.launch {
                    try {
                        val duration = player.prepare(intent.path).coerceAtLeast(0)
                        updateState { AudioPreviewState(ready = true, durationMs = duration) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        fail()
                    }
                }
            }

            AudioPreviewIntent.Stop -> stop()

            AudioPreviewIntent.Toggle -> if (currentState.ready) {
                safely {
                    if (currentState.playing) {
                        player.pause()
                        positionJob?.cancel()
                        updateState { it.copy(playing = false, positionMs = player.positionMs) }
                    } else {
                        if (currentState.positionMs >= currentState.durationMs) player.seek(0)
                        player.play()
                        updateState { it.copy(playing = true) }
                        positionJob = viewModelScope.launch {
                            while (isActive) {
                                delay(100)
                                safely {
                                    val playing = player.playing
                                    val position = if (playing) player.positionMs else currentState.durationMs
                                    updateState {
                                        it.copy(playing = playing, positionMs = position.coerceIn(0, it.durationMs))
                                    }
                                    if (!playing) positionJob?.cancel()
                                }
                            }
                        }
                    }
                }
            }

            is AudioPreviewIntent.Seek -> if (currentState.ready) {
                safely {
                    val position = intent.positionMs.coerceIn(0, currentState.durationMs)
                    player.seek(position)
                    updateState { it.copy(positionMs = position) }
                }
            }
        }
    }
    private inline fun safely(action: () -> Unit) {
        try {
            action()
        } catch (_: Exception) {
            fail()
        }
    }
    private fun fail() {
        stop()
        updateState { AudioPreviewState(error = "Audio could not be played. Check that the file is available.") }
    }
    private fun stop() {
        loadJob?.cancel()
        positionJob?.cancel()
        player.release()
        updateState { AudioPreviewState() }
    }
    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
