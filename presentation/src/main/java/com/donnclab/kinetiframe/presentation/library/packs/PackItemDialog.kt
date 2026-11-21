package com.donnclab.kinetiframe.presentation.library.packs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.presentation.R
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
        title = { Text(stringResource(if (creating) R.string.pack_item_new else R.string.pack_item_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                CompactTextField(name, {
                    name = it.take(com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_NAME_LENGTH)
                }, label = { Text(stringResource(R.string.pack_item_name_short)) }, singleLine = true)
                CompactTextField(description, {
                    description =
                        it.take(com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_DESCRIPTION_LENGTH)
                }, label = { Text(stringResource(R.string.pack_item_description)) }, singleLine = true)
            }
        },
        confirmButton = {
            ContextIconButton(
                ContextIconAction(Icons.Default.Check, stringResource(R.string.pack_item_confirm), {
                    onSave(PackFrameTag(name.trim(), description.trim()))
                }, enabled = PackFrameTag(name, description).isValid),
            )
        },
        dismissButton = {
            ContextIconButton(ContextIconAction(Icons.Default.Close, stringResource(R.string.action_cancel), onDismiss))
        },
    )
}

@Preview
@Composable
private fun PackItemDialogPreview() {
    MaterialTheme { PackItemDialog(PackFrameTag("Smile"), false, {}, {}) }
}
