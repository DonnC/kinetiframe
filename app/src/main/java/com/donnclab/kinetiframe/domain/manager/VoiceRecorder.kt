package com.donnclab.kinetiframe.domain.manager

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.donnclab.kinetiframe.engine.audio.Waveform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Owns the whole native voice-capture lifecycle -- the `MediaRecorder`, the amplitude poll loop
 * and the elapsed clock -- and publishes it as one immutable [State].
 *
 * This is a class rather than composable state because a `MediaRecorder` is a native resource
 * that must be released exactly once. Driven from composable state, a recomposition or an
 * unexpected dismissal could strand a live microphone handle. Hoisting it here also keeps the screen a pure function of [State], which is
 * what lets the meter be previewed without a microphone.
 *
 * No audio dependency is involved: [MediaRecorder.getMaxAmplitude] already reports peak level
 * since the previous call, so polling it on a fixed cadence is a complete live meter, and the
 * normalisation maths lives in [Waveform] where it can be tested on the JVM.
 */
class VoiceRecorder(private val context: Context, private val scope: CoroutineScope) {
    /** Everything the recorder UI renders. [elapsedMicros] is wall-clock: an AAC take of a few
     *  seconds does not drift enough to justify a MediaMetadataRetriever round trip. */
    data class State(
        val isRecording: Boolean = false,
        val elapsedMicros: Long = 0L,
        val waveform: Waveform = Waveform.EMPTY,
        val hasTake: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var pollJob: Job? = null
    private var startedAtMs = 0L

    fun start(outputFile: File) {
        if (_state.value.isRecording) return
        runCatching {
            val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = created
        }.onFailure { throwable ->
            _state.value = State(error = throwable.message ?: "Could not start recording")
            return
        }
        startedAtMs = System.currentTimeMillis()
        _state.value = State(isRecording = true)
        pollJob = scope.launch {
            // The first getMaxAmplitude() after start() reports garbage on some devices because
            // it measures from an undefined baseline, so it is read once and discarded.
            runCatching { recorder?.maxAmplitude }
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val current = recorder ?: break
                val amplitude = runCatching { current.maxAmplitude }.getOrDefault(0)
                _state.value = _state.value.copy(
                    elapsedMicros = (System.currentTimeMillis() - startedAtMs) * 1_000L,
                    waveform = _state.value.waveform.plus(amplitude),
                )
            }
        }
    }

    /** Ends the take and returns its duration, keeping the captured waveform on screen so the
     *  animator can see what they just recorded before committing it. */
    fun stop(): Long {
        if (!_state.value.isRecording) return _state.value.elapsedMicros
        val elapsedMicros = (System.currentTimeMillis() - startedAtMs) * 1_000L
        teardown()
        _state.value = _state.value.copy(isRecording = false, elapsedMicros = elapsedMicros, hasTake = true)
        return elapsedMicros
    }

    /** Abandons the take: stops capture and clears the meter so a re-record starts clean. */
    fun cancel() {
        teardown()
        _state.value = State()
    }

    /** Must be called when the owning screen leaves composition -- a live MediaRecorder is not
     *  reclaimed on its own. */
    fun release() = cancel()

    private fun teardown() {
        pollJob?.cancel()
        pollJob = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }

    private companion object {
        /** ~16 bars a second: dense enough to read as a live meter, cheap enough to be free. */
        const val POLL_INTERVAL_MS = 60L
    }
}
