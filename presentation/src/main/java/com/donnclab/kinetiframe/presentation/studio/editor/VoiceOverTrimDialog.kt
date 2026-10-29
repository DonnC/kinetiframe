// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.media.audio.AudioTrimSelection
import com.donnclab.kinetiframe.media.audio.TrimPreviewPlayer
import com.donnclab.kinetiframe.media.audio.Waveform
import com.donnclab.kinetiframe.presentation.media.audio.AudioTrimActions
import com.donnclab.kinetiframe.presentation.media.audio.AudioTrimScreen
import com.donnclab.kinetiframe.presentation.media.audio.AudioTrimState
import com.donnclab.kinetiframe.presentation.media.audio.PendingAudioTake
import kotlinx.coroutines.CoroutineScope

/**
 * Trims a recorded [take] before it is placed. Dismissing deletes the take's file and calls
 * [onDismiss]; confirming passes the chosen selection to [onConfirm], which keeps the file.
 */
@Composable
internal fun VoiceOverTrimDialog(
    take: PendingAudioTake,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
    onConfirm: (AudioTrimSelection) -> Unit,
) {
    val previewPlayer = remember(take) { TrimPreviewPlayer(scope) }
    val previewState by previewPlayer.state.collectAsStateWithLifecycle()
    // A take shorter than the minimum selection cannot be trimmed meaningfully; guard the
    // selection's own requirement rather than letting it throw on construction.
    val sourceDuration = take.durationMicros.coerceAtLeast(AudioTrimSelection.MIN_SELECTION_MICROS)
    var selection by remember(take) { mutableStateOf(AudioTrimSelection(sourceDuration)) }
    DisposableEffect(take) { onDispose { previewPlayer.release() } }

    fun dismissTake() {
        previewPlayer.release()
        runCatching { take.file.delete() }
        onDismiss()
    }

    Dialog(onDismissRequest = { dismissTake() }) {
        AudioTrimScreen(
            state = AudioTrimState(
                waveform = Waveform(take.peaks),
                selection = selection,
                playheadMicros = previewState.positionMicros,
                isPreviewing = previewState.isPlaying,
            ),
            actions = AudioTrimActions(
                onSelectionChange = { updated ->
                    selection = updated
                    // Dragging a handle changes what should be sounding, so the preview stops
                    // rather than playing the old range.
                    if (previewState.isPlaying) previewPlayer.stop()
                },
                onTogglePreview = {
                    if (previewState.isPlaying) {
                        previewPlayer.stop()
                    } else {
                        previewPlayer.play(take.file.absolutePath, selection)
                    }
                },
                onConfirm = {
                    previewPlayer.release()
                    onConfirm(selection)
                },
                onCancel = { dismissTake() },
            ),
        )
    }
}
