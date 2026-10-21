package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.engine.rigging.model.ActorView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailsSheet(
    actor: ActorManifest,
    thumbnail: String?,
    performances: List<PerformanceDocument>,
    onDismiss: () -> Unit,
    onEditView: (ActorView) -> Unit,
    onOpenProject: (String) -> Unit,
    projectNames: Map<String, String> = emptyMap(),
) {
    var tab by remember(actor.actorId) { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    thumbnail?.let {
                        AsyncImage(
                            java.io.File(it),
                            actor.displayName,
                            Modifier.size(88.dp).padding(6.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        actor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${actor.authoredViews().size} views · ${performances.size} performances",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TabRow(tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Views · ${actor.authoredViews().size}") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Performances · ${performances.size}") })
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (tab == 0) {
                    items(actor.authoredViews().keys.toList(), key = { it.name }) { view ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onEditView(view)
                            },
                            headlineContent = { Text(view.title, style = MaterialTheme.typography.bodyMedium) },
                            trailingContent = {
                                IconButton(onClick = {
                                    onEditView(view)
                                }) { Icon(Icons.Default.Edit, "Edit ${view.title}") }
                            },
                        )
                    }
                } else {
                    if (performances.isEmpty()) {
                        item {
                            Text(
                                "No saved performances",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(performances, key = { "${it.projectId}:${it.performanceId}" }) { performance ->
                        val view = actor.authoredViews().entries.firstOrNull {
                            it.value == performance.actorRevisionId
                        }?.key
                        ListItem(
                            modifier = Modifier.clickable { onOpenProject(performance.projectId) },
                            headlineContent = {
                                Text(
                                    performance.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(
                                    "${view?.title ?: "Saved view"} · ${String.format(
                                        java.util.Locale.ROOT,
                                        "%.1f",
                                        performance.durationMicros / 1_000_000.0,
                                    )} s\n${projectNames[performance.projectId] ?: "Saved project"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    onOpenProject(performance.projectId)
                                }) { Icon(Icons.Default.Movie, "Open owning project") }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CharacterDetailsPreview() {
    CharacterDetailsSheet(
        ActorManifest(
            actorId = "actor-preview",
            displayName = "Tavi",
            templateId = ActorManifest.HUMAN_TEMPLATE_ID,
            currentRevisionId = "revision-preview",
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0,
            viewRevisions = mapOf(ActorView.FRONT to "revision-preview"),
        ),
        null,
        emptyList(),
        {},
        {},
        {},
    )
}
