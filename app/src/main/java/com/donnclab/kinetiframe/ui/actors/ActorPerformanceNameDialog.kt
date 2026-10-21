package com.donnclab.kinetiframe.ui.actors

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.ui.components.common.CompactTextField

@Composable
fun ActorPerformanceNameDialog(
    initialName: String = "New performance",
    title: String = "Save performance",
    confirmLabel: String = "Save & Return",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            CompactTextField(name, { name = it }, label = { Text("Performance name") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun ActorPerformanceNameDialogPreview() = ActorPerformanceNameDialog(onDismiss = {}, onSave = {})
