package com.donnclab.kinetiframe.studio.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Cancel/dismiss never navigates. Save callers exit only after durable persistence succeeds. */
@Composable
fun SaveBeforeExitDialog(onSave: () -> Unit, onDiscard: (() -> Unit)?, onCancel: () -> Unit, canSave: Boolean = true) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save before leaving?") },
        text = {
            Text(
                if (onDiscard == null) {
                    "Save your work before leaving, or cancel to keep editing."
                } else {
                    "Save your work, discard unsaved changes, or keep editing."
                },
            )
        },
        confirmButton = { TextButton(onSave, enabled = canSave) { Text("Save") } },
        dismissButton = {
            Row {
                onDiscard?.let { TextButton(it) { Text("Discard") } }
                TextButton(onCancel) { Text("Cancel") }
            }
        },
    )
}

@Preview
@Composable
private fun SaveBeforeExitPreview() {
    KinetiFrameTheme { SaveBeforeExitDialog({}, {}, {}) }
}
