// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.home

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.MediaCard

/** Drag label for a project being dragged onto another project or a collection. */
private const val PROJECT_DRAG_LABEL = "kinetiframe-project"

/** One collection on Home: its name and its projects, newest first. */
internal data class ProjectCollection(val name: String, val projects: List<ProjectManifest>)

/** A project card that can be long-pressed and dragged, carrying [key], and that accepts a dropped project. */
@Composable
internal fun Modifier.projectDragAndDrop(key: String, onDrop: (draggedKey: String) -> Unit): Modifier =
    projectDragSource(key).projectDropTarget(key, onDrop)

/** Long-pressing starts dragging the project with organisation [key]. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.projectDragSource(key: String): Modifier = dragAndDropSource { _ ->
    DragAndDropTransferData(
        ClipData(PROJECT_DRAG_LABEL, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), ClipData.Item(key)),
    )
}

/** Accepts a dragged project, calling [onDrop] with its key; outlined while one hovers. [key] is this target's own. */
@Composable
internal fun Modifier.projectDropTarget(key: String, onDrop: (draggedKey: String) -> Unit): Modifier {
    var hovering by remember { mutableStateOf(false) }
    val target = remember(key) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                hovering = true
            }

            override fun onExited(event: DragAndDropEvent) {
                hovering = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                hovering = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovering = false
                val dragged = event.draggedProjectKey() ?: return false
                if (dragged != key) onDrop(dragged)
                return dragged != key
            }
        }
    }
    val outline = if (hovering) {
        Modifier.border(
            Dimens.borderThick,
            MaterialTheme.colorScheme.primary,
            RoundedCornerShape(HomeTokens.cardCorner),
        )
    } else {
        Modifier
    }
    return this.then(outline).dragAndDropTarget(shouldStartDragAndDrop = { it.carriesProject() }, target = target)
}

private fun DragAndDropEvent.carriesProject(): Boolean = mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
    toAndroidDragEvent().clipDescription?.label == PROJECT_DRAG_LABEL

private fun DragAndDropEvent.draggedProjectKey(): String? =
    toAndroidDragEvent().clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

/** Collections as cards: the newest project's picture, the name and how many projects it holds. */
@Composable
internal fun CollectionGrid(
    collections: List<ProjectCollection>,
    onOpen: (String) -> Unit,
    onDropProject: (draggedKey: String, collection: String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.spaceL,
            end = Dimens.spaceL,
            top = Dimens.spaceXs,
            bottom = HomeTokens.listBottomReserve,
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
    ) {
        items(collections, key = { it.name }) { collection ->
            CollectionCard(collection, {
                onOpen(collection.name)
            }, { dragged -> onDropProject(dragged, collection.name) })
        }
    }
}

@Composable
private fun CollectionCard(collection: ProjectCollection, onOpen: () -> Unit, onDrop: (String) -> Unit) {
    val count = collection.projects.size
    MediaCard(
        title = collection.name,
        subtitle = pluralStringResource(R.plurals.home_collection_count, count, count),
        onClick = onOpen,
        modifier = Modifier.projectDropTarget("collection:${collection.name}") { onDrop(it) },
        media = {
            val cover = collection.projects.firstOrNull()?.thumbnailPath
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
        },
    )
}

/** The row above an open collection's projects: back to all collections, and its name. */
@Composable
internal fun OpenCollectionHeader(name: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.home_collections_all))
        }
        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Names the collection made by dropping one project onto another. */
@Composable
internal fun NewCollectionDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val defaultName = stringResource(R.string.home_collection_default_name)
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_collection_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_COLLECTION_NAME) },
                label = { Text(stringResource(R.string.home_collection_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.home_collection_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val MAX_COLLECTION_NAME = 60
