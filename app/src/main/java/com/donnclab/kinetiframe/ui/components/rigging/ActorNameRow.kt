package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.common.CompactTextField

/** The actor's name, tap-to-rename. One of the sections composed by [PuppetInspector]. */
@Composable
fun ActorNameRow(actorName: String, onIntent: (TimelineIntent) -> Unit) {
    var editingName by remember { mutableStateOf(false) }
    var currentName by remember(actorName) { mutableStateOf(actorName) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp).clickable { editingName = true },
        ) {
            Icon(
                Lucide.Folder,
                null,
                tint = ActorStudioTokens.accent,
                modifier = Modifier.size(18.dp).padding(end = 6.dp),
            )
            Text(
                actorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ActorStudioTokens.onPanel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (editingName) {
            AlertDialog(
                onDismissRequest = { editingName = false },
                title = { Text("Rename Actor") },
                text = {
                    CompactTextField(value = currentName, onValueChange = { currentName = it }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = {
                        onIntent(TimelineIntent.Actor.RenameActor(currentName))
                        editingName = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel") } },
            )
        }
    }
}
