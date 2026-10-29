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
import com.donnclab.kinetiframe.presentation.studio.dialogs.EditorErrorDialog

/** Messages the editor reports to the animator. Each one shows as a dialog until it is dismissed. */
@Stable
internal class EditorMessages {
    var placementError by mutableStateOf<String?>(null)
    var actorLibraryMessage by mutableStateOf<String?>(null)
    var backgroundImageError by mutableStateOf<String?>(null)
    var timelineError by mutableStateOf<String?>(null)
    var projectSaveMessage by mutableStateOf<String?>(null)
    var audioError by mutableStateOf<String?>(null)
    var videoError by mutableStateOf<String?>(null)
    var libraryError by mutableStateOf<String?>(null)
    var libraryMessage by mutableStateOf<String?>(null)
}

/** Shows every pending message except [EditorMessages.placementError], which the editor shows first. */
@Composable
internal fun EditorMessageDialogs(messages: EditorMessages) {
    messages.actorLibraryMessage?.let { MessageDialog("Actor library", it) { messages.actorLibraryMessage = null } }
    messages.backgroundImageError?.let {
        MessageDialog("Background image", it) { messages.backgroundImageError = null }
    }
    TimelineErrorDialog(messages)
    messages.projectSaveMessage?.let { MessageDialog("Save Project", it) { messages.projectSaveMessage = null } }
    messages.audioError?.let { MessageDialog("Audio", it) { messages.audioError = null } }
    messages.videoError?.let { MessageDialog("Video", it) { messages.videoError = null } }
    messages.libraryError?.let { MessageDialog("Library", it) { messages.libraryError = null } }
    messages.libraryMessage?.let { MessageDialog("Library", it) { messages.libraryMessage = null } }
}

@Composable
internal fun TimelineErrorDialog(messages: EditorMessages) {
    messages.timelineError?.let { message -> EditorErrorDialog(message) { messages.timelineError = null } }
}

@Composable
internal fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
