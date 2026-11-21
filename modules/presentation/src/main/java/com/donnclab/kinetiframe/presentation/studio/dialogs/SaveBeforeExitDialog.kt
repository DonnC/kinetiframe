package com.donnclab.kinetiframe.presentation.studio.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/** Cancel/dismiss never navigates. Save callers exit only after durable persistence succeeds. */
@Composable
fun SaveBeforeExitDialog(onSave: () -> Unit, onDiscard: (() -> Unit)?, onCancel: () -> Unit, canSave: Boolean = true) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.editor_exit_title)) },
        text = {
            Text(
                if (onDiscard == null) {
                    stringResource(R.string.editor_exit_message_no_discard)
                } else {
                    stringResource(R.string.editor_exit_message)
                },
            )
        },
        confirmButton = { TextButton(onSave, enabled = canSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                onDiscard?.let { TextButton(it) { Text(stringResource(R.string.action_discard)) } }
                TextButton(onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Preview
@Composable
private fun SaveBeforeExitPreview() {
    KinetiFrameTheme { SaveBeforeExitDialog({}, {}, {}) }
}
