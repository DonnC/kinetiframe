package com.donnclab.kinetiframe.presentation.library.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.AssetCategory
import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.presentation.common.CompactTextField

@Composable
fun PackOrganizationDialog(
    name: String,
    initial: AssetOrganization,
    onDismiss: () -> Unit,
    onSave: (AssetOrganization) -> Unit,
) {
    var displayName by remember { mutableStateOf(initial.displayName ?: name) }
    var collection by remember { mutableStateOf(initial.collection) }
    var category by remember { mutableStateOf(initial.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Organise", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTextField(displayName, {
                    displayName = it.take(60)
                }, singleLine = true, label = { Text("Library name") })
                CompactTextField(collection, {
                    collection = it.take(60)
                }, singleLine = true, label = { Text("Collection") })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(AssetCategory.entries) { value ->
                        FilterChip(
                            category == value,
                            { category = value },
                            label = { Text(value.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            IconButton(
                enabled = displayName.isNotBlank() && collection.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            displayName = displayName.trim(),
                            collection = collection.trim(),
                            category = category,
                        ),
                    )
                },
            ) {
                Icon(Icons.Default.Check, "Save organisation")
            }
        },
        dismissButton = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PackOrganizationDialogPreview() {
    KinetiFrameTheme {
        PackOrganizationDialog("Tavi · Mouths", AssetOrganization("Loose Signal", AssetCategory.EXPRESSIONS), {}, {})
    }
}
