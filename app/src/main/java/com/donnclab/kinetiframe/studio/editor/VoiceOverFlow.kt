// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.presentation.media.audio.PendingAudioTake
import com.donnclab.kinetiframe.studio.StudioViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Record a take, trim it, then place it on the first free audio track at the current frame. */
@Composable
internal fun VoiceOverFlow(
    viewModel: StudioViewModel,
    media: EditorMediaImporter,
    ui: EditorUiState,
    scope: CoroutineScope,
) {
    val panels = ui.panels
    // A take waiting to be trimmed. Clearing it discards the take without placing anything.
    var pendingTake by remember { mutableStateOf<PendingAudioTake?>(null) }
    if (panels.showRecordAudioDialog) {
        VoiceOverRecorderDialog(
            scope,
            onClose = { panels.showRecordAudioDialog = false },
            onTake = { take ->
                panels.showRecordAudioDialog = false
                pendingTake = take
            },
        )
    }
    pendingTake?.let { take ->
        VoiceOverTrimDialog(
            take,
            scope,
            onDismiss = { pendingTake = null },
            onConfirm = { selection ->
                pendingTake = null
                scope.launch {
                    media.keepTake(take).fold(
                        onSuccess = { (assetId, destination) ->
                            viewModel.handleIntent(
                                viewModel.state.value.addTrimmedAudioClip(
                                    assetId,
                                    destination.absolutePath,
                                    selection,
                                    take.peaks,
                                ),
                            )
                        },
                        onFailure = { ui.messages.audioError = it.message ?: "Could not save the recording." },
                    )
                }
            },
        )
    }
}
