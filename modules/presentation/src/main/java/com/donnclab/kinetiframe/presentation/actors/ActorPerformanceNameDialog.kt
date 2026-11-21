package com.donnclab.kinetiframe.presentation.actors

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.CompactTextField

@Composable
fun ActorPerformanceNameDialog(
    initialName: String = stringResource(R.string.actor_new_performance),
    title: String = stringResource(R.string.puppet_save_performance),
    confirmLabel: String = stringResource(R.string.actor_save_and_return),
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            CompactTextField(name, {
                name = it
            }, label = { Text(stringResource(R.string.actor_performance_name)) }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview
@Composable
private fun ActorPerformanceNameDialogPreview() = ActorPerformanceNameDialog(onDismiss = {}, onSave = {})
