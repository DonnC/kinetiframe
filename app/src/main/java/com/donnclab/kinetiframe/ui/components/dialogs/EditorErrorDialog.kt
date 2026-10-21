package com.donnclab.kinetiframe.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun EditorErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Studio") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun EditorErrorPreview() {
    KinetiFrameTheme { EditorErrorDialog("Saved background image is missing.", {}) }
}
