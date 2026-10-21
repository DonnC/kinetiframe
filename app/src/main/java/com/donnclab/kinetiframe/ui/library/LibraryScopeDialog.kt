package com.donnclab.kinetiframe.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.data.actor.LibraryScope

/**
 * The Project-vs-Personal save choice -- shown right before an export actually runs, so every
 * "Add to Library" action gets the same choice, with no separate "Vault" UI concept.
 * [itemLabel] names the specific thing being saved (e.g. "\"Voice-over\"", or
 * "this background image") so the dialog's body reads naturally either way.
 */
@Composable
fun LibraryScopeDialog(itemLabel: String, onDismiss: () -> Unit, onChoose: (LibraryScope) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to library") },
        text = {
            Column(Modifier.padding(top = 4.dp)) {
                Text(
                    "Save $itemLabel to your Personal library (every project can use it) or just this project's own vault?",
                )
            }
        },
        confirmButton = { TextButton(onClick = { onChoose(LibraryScope.PERSONAL) }) { Text("Personal") } },
        dismissButton = { TextButton(onClick = { onChoose(LibraryScope.PROJECT) }) { Text("This Project") } },
    )
}
