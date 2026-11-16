// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.presentation.studio.dialogs.EditorErrorDialog

/** Messages the editor reports to the animator. Each one shows as a dialog until it is dismissed. */
@Stable
internal class EditorMessages {
    var placementError by mutableStateOf<String?>(null)
    var actorLibraryMessage by mutableStateOf<String?>(null)
    var backgroundImageError by mutableStateOf<String?>(null)
    var timelineError by mutableStateOf<UiText?>(null)
    var projectSaveMessage by mutableStateOf<UiText?>(null)
    var audioError by mutableStateOf<String?>(null)
    var videoError by mutableStateOf<String?>(null)
    var libraryError by mutableStateOf<String?>(null)
    var libraryMessage by mutableStateOf<String?>(null)
}

/** Shows every pending message except [EditorMessages.placementError], which the editor shows first. */
@Composable
internal fun EditorMessageDialogs(messages: EditorMessages) {
    messages.actorLibraryMessage?.let {
        MessageDialog(stringResource(R.string.editor_actor_library), it) {
            messages.actorLibraryMessage =
                null
        }
    }
    messages.backgroundImageError?.let {
        MessageDialog(stringResource(R.string.editor_background_image), it) { messages.backgroundImageError = null }
    }
    TimelineErrorDialog(messages)
    messages.projectSaveMessage?.let {
        MessageDialog(stringResource(R.string.editor_save_project), it.asString()) {
            messages.projectSaveMessage =
                null
        }
    }
    messages.audioError?.let {
        MessageDialog(stringResource(R.string.library_kind_audio), it) {
            messages.audioError =
                null
        }
    }
    messages.videoError?.let { MessageDialog(stringResource(R.string.tracks_video), it) { messages.videoError = null } }
    messages.libraryError?.let {
        MessageDialog(stringResource(R.string.library_title), it) {
            messages.libraryError =
                null
        }
    }
    messages.libraryMessage?.let {
        MessageDialog(stringResource(R.string.library_title), it) {
            messages.libraryMessage =
                null
        }
    }
}

@Composable
internal fun TimelineErrorDialog(messages: EditorMessages) {
    messages.timelineError?.let { message -> EditorErrorDialog(message.asString()) { messages.timelineError = null } }
}

@Composable
internal fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
