package com.donnclab.kinetiframe.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun ExitConfirmationDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit KinetiFrame?") },
        text = { Text("You can return to your saved projects whenever you're ready.") },
        confirmButton = { TextButton(onClick = onExit) { Text("Exit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } },
    )
}

@Preview
@Composable
private fun ExitConfirmationPreview() {
    KinetiFrameTheme { ExitConfirmationDialog({}, {}) }
}
