package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.library.model.AssetCategory
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition

@Composable
fun CollectionBrowserControls(
    position: BrowserPosition,
    collections: List<String>,
    onChange: (BrowserPosition) -> Unit,
    showCategories: Boolean = false,
) {
    val names = collections.distinct().sorted()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            BrowseMode.entries.forEach { mode ->
                val active = position.mode == mode
                Surface(
                    onClick = {
                        onChange(
                            position.copy(
                                mode = mode,
                                collection = if (mode == BrowseMode.COLLECTIONS) position.collection else null,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f).height(40.dp).semantics { selected = active },
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(mode.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        if (position.mode == BrowseMode.COLLECTIONS || showCategories) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (position.mode == BrowseMode.COLLECTIONS) {
                    BrowserFilterMenu(
                        label = position.collection ?: "All collections",
                        icon = Icons.Default.FolderOpen,
                        options = listOf("All collections") + names,
                        selectedIndex = position.collection?.let { names.indexOf(it) + 1 } ?: 0,
                        modifier = Modifier.weight(1f),
                        onSelect = { index ->
                            onChange(
                                position.copy(
                                    collection = if (index ==
                                        0
                                    ) {
                                        null
                                    } else {
                                        names[index - 1]
                                    },
                                ),
                            )
                        },
                    )
                }
                if (showCategories) {
                    BrowserFilterMenu(
                        label = position.category?.label ?: "All categories",
                        icon = Icons.Default.Category,
                        options = listOf("All categories") + AssetCategory.entries.map { it.label },
                        selectedIndex = position.category?.ordinal?.plus(1) ?: 0,
                        modifier = Modifier.weight(1f),
                        onSelect = { index ->
                            onChange(
                                position.copy(
                                    category = if (index ==
                                        0
                                    ) {
                                        null
                                    } else {
                                        AssetCategory.entries[index - 1]
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CollectionBrowserPreview() {
    MaterialTheme {
        CollectionBrowserControls(BrowserPosition(BrowseMode.COLLECTIONS), listOf("Loose Signal", "Education"), {})
    }
}
