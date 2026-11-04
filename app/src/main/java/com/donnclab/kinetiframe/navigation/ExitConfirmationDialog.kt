package com.donnclab.kinetiframe.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun ExitConfirmationDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_title)) },
        text = { Text(stringResource(R.string.exit_message)) },
        confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.exit_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.exit_stay)) } },
    )
}

@Preview
@Composable
private fun ExitConfirmationPreview() {
    KinetiFrameTheme { ExitConfirmationDialog({}, {}) }
}
