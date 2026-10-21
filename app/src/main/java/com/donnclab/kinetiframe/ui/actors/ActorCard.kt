package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.data.actor.ActorManifest

/** One compact dashboard card; its artwork is a thumbnail, never an editable rig surface. */
@Composable
fun ActorCard(
    actor: ActorManifest,
    thumbnailPath: String? = null,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onArchive: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().height(112.dp)) {
                if (thumbnailPath == null) {
                    com.donnclab.kinetiframe.ui.components.rigging.RigBlueprintThumbnail(
                        com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate.HUMAN,
                        Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = "file://$thumbnailPath",
                        contentDescription = "${actor.displayName} actor preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Actor actions") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                            menuOpen = false
                            onRename()
                        })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                            menuOpen = false
                            onDuplicate()
                        })
                        DropdownMenuItem(text = { Text("Add to library") }, onClick = {
                            menuOpen = false
                            onExport()
                        })
                        DropdownMenuItem(text = { Text("Archive") }, onClick = {
                            menuOpen = false
                            onArchive()
                        })
                    }
                }
            }
            Text(actor.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                "Actor · revision ${actor.currentRevisionId.takeLast(6)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Preview(widthDp = 180)
@Composable
private fun ActorCardPreview() = ActorCard(
    ActorManifest(
        actorId = "actor-alice",
        displayName = "Alice",
        templateId = ActorManifest.HUMAN_TEMPLATE_ID,
        currentRevisionId = "revision-1",
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    ),
    null,
    {},
    {},
    {},
    {},
    {},
)
