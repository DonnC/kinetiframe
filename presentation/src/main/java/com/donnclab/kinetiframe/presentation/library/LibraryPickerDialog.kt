package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Square
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.ui.appViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The shared "add from Library" surface, opened from the unified Add menu
 * ([com.donnclab.kinetiframe.presentation.studio.timeline.ActorPerformancePicker]) and from the project gallery's
 * import action. [allowedKinds] narrows what's pickable to whatever the calling context can
 * actually use (e.g. the in-editor Add menu excludes [LibraryItemKind.PROJECT], since importing a
 * whole project only makes sense from the gallery, not while one is already open) -- null shows
 * everything, for a general browse/manage use.
 *
 * [projectId], when non-null, is what makes the Project-vs-Personal split show up
 * here at all: with it, the LIBRARY tab gains a "This Project" / "Personal" segmented toggle
 * ([LibraryScope]), browsing that project's own `vault/` alongside the cross-project `library/`
 * root -- omit it (the gallery's own import flow does) and the picker only ever shows PERSONAL
 * items, since there is no "current project" to scope to there.
 *
 * [onPick] receives the chosen item's manifest, which records its scope. Routing what
 * a picked item actually *does* (place an audio clip, set a background, stamp a drawing onto the
 * current frame, ...) is the caller's job, mirroring how this whole system keeps "what a file
 * means" separate from "where the file lives" everywhere else in this codebase.
 *
 * A [LibraryItemKind.PACK] item never reaches [onPick] at all -- per its own usage model (a
 * floating, dockable tray a user drags items out of onto the canvas repeatedly, not a one-shot
 * pick-and-close), tapping one dismisses this dialog and calls [onOpenPack] instead, handing the
 * caller the pack's manifest so it can open [com.donnclab.kinetiframe.presentation.library.packs.PackTray].
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LibraryPickerDialog(
    allowedKinds: Set<LibraryItemKind>? = null,
    projectId: String? = null,
    dismissOnOpenPack: Boolean = true,
    onDismiss: () -> Unit,
    onOpenPack: (LibraryItemManifest) -> Unit = {},
    onPick: (LibraryItemManifest) -> Unit,
) {
    val model: LibraryPickerViewModel = appViewModel(key = "library-picker-$projectId") {
        projectId?.let { set(LibraryPickerViewModel.ProjectIdKey, it) }
    }
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.handleIntent(LibraryPickerIntent.Open) }
    val scope = state.scope
    val kindFilter = state.kindFilter

    val visibleItems = remember(state.items, kindFilter, allowedKinds) {
        state.items.filter {
            it.kind != LibraryItemKind.PALETTE && (allowedKinds == null || it.kind in allowedKinds) &&
                (kindFilter == null || it.kind == kindFilter)
        }
    }
    val filterableKinds = remember(state.items, allowedKinds) {
        (allowedKinds ?: LibraryItemKind.entries.toSet()).filter { kind ->
            kind != LibraryItemKind.PALETTE &&
                state.items.any { it.kind == kind }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("My Library", style = MaterialTheme.typography.titleMedium)
                if (projectId != null) {
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = scope == LibraryScope.PERSONAL,
                            onClick = { model.handleIntent(LibraryPickerIntent.SelectScope(LibraryScope.PERSONAL)) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("Personal") }
                        SegmentedButton(
                            selected = scope == LibraryScope.PROJECT,
                            onClick = { model.handleIntent(LibraryPickerIntent.SelectScope(LibraryScope.PROJECT)) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("This Project") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (filterableKinds.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = kindFilter == null, onClick = {
                                model.handleIntent(LibraryPickerIntent.Filter(null))
                            }, label = { Text("All") })
                        }
                        items(filterableKinds) { kind ->
                            FilterChip(selected = kindFilter == kind, onClick = {
                                model.handleIntent(LibraryPickerIntent.Filter(kind))
                            }, label = { Text(kindLabel(kind)) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        kindFilter == LibraryItemKind.PACK ->
                            PackPickerBrowser(scope, projectId, { pack ->
                                openPackFromPicker(dismissOnOpenPack, onDismiss) { onOpenPack(pack) }
                            })

                        state.loading -> CircularProgressIndicator()

                        state.error != null -> Text(
                            state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp),
                        )

                        visibleItems.isEmpty() -> Text(
                            "Nothing here yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        else -> LazyVerticalGrid(
                            GridCells.Adaptive(140.dp),
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(visibleItems, key = { it.libraryItemId }) { libraryItem ->
                                LibraryItemCard(
                                    libraryItem,
                                    loadThumbnail = model::previewImage,
                                    onClick = {
                                        if (libraryItem.kind == LibraryItemKind.PACK) {
                                            openPackFromPicker(dismissOnOpenPack, onDismiss) { onOpenPack(libraryItem) }
                                        } else {
                                            onPick(libraryItem)
                                        }
                                    },
                                    onPreview = if (libraryItem.kind == LibraryItemKind.AUDIO) {
                                        ({ model.handleIntent(LibraryPickerIntent.Preview(libraryItem)) })
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }

    state.preview?.let { preview ->
        LibraryAudioPreviewDialog(preview, { model.handleIntent(LibraryPickerIntent.ClosePreview) }, {
            model.handleIntent(LibraryPickerIntent.ClosePreview)
            onPick(preview.item)
        })
    }
}

/** [loadThumbnail] finds the card's image off the main thread; without it the card has none. */
@Composable
internal fun LibraryItemCard(
    item: LibraryItemManifest,
    onClick: () -> Unit,
    onPreview: (() -> Unit)? = null,
    loadThumbnail: ((LibraryItemManifest) -> File?)? = null,
) {
    val thumbnail by produceState<File?>(null, item, loadThumbnail) {
        value = loadThumbnail?.let { load -> withContext(Dispatchers.IO) { load(item) } }
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            thumbnail?.let { LibraryImageThumbnail(it, item.displayName) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(item.kind), null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                onPreview?.let { preview ->
                    IconButton(onClick = preview) {
                        Icon(androidx.compose.material.icons.Icons.Filled.PlayArrow, "Preview audio")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.displayName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                kindLabel(item.kind),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (item.tags.isNotEmpty()) {
                Text(
                    item.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun kindLabel(kind: LibraryItemKind): String = when (kind) {
    LibraryItemKind.ACTOR -> "Actors"
    LibraryItemKind.PERFORMANCE -> "Performances"
    LibraryItemKind.BUNDLE -> "Bundles"
    LibraryItemKind.AUDIO -> "Audio"
    LibraryItemKind.BACKGROUND_IMAGE -> "Backgrounds"
    LibraryItemKind.FRAME -> "Frames"
    LibraryItemKind.PROJECT -> "Projects"
    LibraryItemKind.PACK -> "Packs"
    LibraryItemKind.PALETTE -> "Palettes"
}

private fun iconFor(kind: LibraryItemKind): ImageVector = when (kind) {
    LibraryItemKind.ACTOR, LibraryItemKind.PERFORMANCE -> Lucide.Drama
    LibraryItemKind.BUNDLE -> Lucide.Folder
    LibraryItemKind.AUDIO -> Lucide.Music
    LibraryItemKind.BACKGROUND_IMAGE, LibraryItemKind.FRAME -> Lucide.Square
    LibraryItemKind.PROJECT -> Lucide.Clapperboard
    LibraryItemKind.PACK -> Lucide.Shapes
    LibraryItemKind.PALETTE -> Lucide.Shapes
}
