package com.donnclab.kinetiframe.library.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.common.ui.CompactTextField
import com.donnclab.kinetiframe.library.data.AssetOrganization

@Composable
fun CollectionAssignmentDialog(initial: AssetOrganization, onDismiss: () -> Unit, onSave: (AssetOrganization) -> Unit) {
    var collection by remember(initial) { mutableStateOf(initial.collection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Collection", style = MaterialTheme.typography.titleMedium)
        },
        text = { CompactTextField(collection, { collection = it.take(60) }, singleLine = true) },
        confirmButton = {
            IconButton(enabled = collection.isNotBlank(), onClick = {
                onSave(initial.copy(collection = collection.trim()))
            }) {
                Icon(Icons.Default.Check, "Save collection")
            }
        },
        dismissButton = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") } },
    )
}

@Preview
@Composable
private fun CollectionAssignmentPreview() {
    CollectionAssignmentDialog(AssetOrganization("Loose Signal"), {}, {})
}
