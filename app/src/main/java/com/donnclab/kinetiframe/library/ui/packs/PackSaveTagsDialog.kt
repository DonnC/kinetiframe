package com.donnclab.kinetiframe.library.ui.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextIconButton

/** Tags belong to saving, not to the permanent drawing workspace. */
@Composable
fun PackSaveTagsDialog(
    totalFrames: Int,
    tags: Map<Int, PackFrameTag>,
    currentFrame: Int,
    onDismiss: () -> Unit,
    onSave: (Map<Int, PackFrameTag>) -> Unit,
) {
    val draft = remember {
        mutableStateMapOf<Int, PackFrameTag>().apply {
            putAll(packSaveTags(totalFrames, tags))
        }
    }
    var selected by remember { mutableIntStateOf(currentFrame.coerceIn(0, totalFrames - 1)) }
    val tag = draft.getValue(selected)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Save pack") }, text = {
        Column(
            Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(totalFrames) { index ->
                    FilterChip(selected == index, { selected = index }, label = { Text("${index + 1}") })
                }
            }
            CompactTextField(
                tag.name,
                {
                    draft[selected] =
                        tag.copy(name = it.take(com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_NAME_LENGTH))
                },
                label = { Text("Item name") },
                singleLine = true,
            )
            CompactTextField(
                tag.description,
                {
                    draft[selected] =
                        tag.copy(
                            description = it.take(
                                com.donnclab.kinetiframe.library.model.PackItemEntry.MAX_DESCRIPTION_LENGTH,
                            ),
                        )
                },
                label = { Text("Description (optional)") },
                singleLine = true,
            )
        }
    }, confirmButton = {
        ContextIconButton(
            ContextIconAction(
                Icons.Default.Save,
                "Save pack",
                { onSave(draft.toMap()) },
                enabled = draft.values.all { it.isValid },
            ),
        )
    }, dismissButton = { ContextIconButton(ContextIconAction(Icons.Default.Close, "Cancel", onDismiss)) })
}

@Preview
@Composable
private fun PackSaveTagsDialogPreview() {
    PackSaveTagsDialog(3, emptyMap(), 0, {}, {})
}
