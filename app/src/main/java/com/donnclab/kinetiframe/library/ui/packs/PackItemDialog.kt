package com.donnclab.kinetiframe.library.ui.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton

/** One item's metadata is requested before insertion; cancellation cannot create a blank item. */
@Composable
fun PackItemDialog(initial: PackFrameTag, creating: Boolean, onDismiss: () -> Unit, onSave: (PackFrameTag) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var description by remember(initial) { mutableStateOf(initial.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "New item" else "Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTextField(name, {
                    name = it.take(com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_NAME_LENGTH)
                }, label = { Text("Name") }, singleLine = true)
                CompactTextField(description, {
                    description =
                        it.take(com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_DESCRIPTION_LENGTH)
                }, label = { Text("Description (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            ContextIconButton(
                ContextIconAction(Icons.Default.Check, "Confirm item", {
                    onSave(PackFrameTag(name.trim(), description.trim()))
                }, enabled = PackFrameTag(name, description).isValid),
            )
        },
        dismissButton = { ContextIconButton(ContextIconAction(Icons.Default.Close, "Cancel", onDismiss)) },
    )
}

@Preview
@Composable
private fun PackItemDialogPreview() {
    MaterialTheme { PackItemDialog(PackFrameTag("Smile"), false, {}, {}) }
}
