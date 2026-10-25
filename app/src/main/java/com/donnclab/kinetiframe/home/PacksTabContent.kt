package com.donnclab.kinetiframe.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserControls
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserIndex
import com.donnclab.kinetiframe.library.ui.packs.PackDetailsScreen
import com.donnclab.kinetiframe.library.ui.packs.PackDetailsState
import com.donnclab.kinetiframe.library.ui.packs.PackOrganizationDialog
import com.donnclab.kinetiframe.library.ui.packs.PacksTabCallbacks
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabState
import com.donnclab.kinetiframe.presentation.actors.ActorManagerMessage
import java.io.File

/**
 * Home's Packs tab: the saved-pack gallery, its delete confirmation, and the drill-down overlay a
 * card opens.
 *
 * `internal` because it is part of Home, not a reusable component, and shares Home's
 * LoadingBox and EmptySearchMessage states.
 */
@Composable
internal fun PacksTabContent(
    state: PacksTabState,
    previewFiles: Map<String, List<File>>,
    query: String,
    callbacks: PacksTabCallbacks,
) {
    val onIntent = callbacks.onIntent
    val onShare = callbacks.onShare
    val onRequestCreate = callbacks.onRequestCreate
    val onEdit = callbacks.onEdit
    val detailsPreviewFile = callbacks.detailsPreviewFile
    var deleteCandidate by remember { mutableStateOf<LibraryItemManifest?>(null) }
    var organizeCandidate by remember { mutableStateOf<LibraryItemManifest?>(null) }
    val filtered = remember(state.packs, query, state.itemNames, state.organization, state.browser) {
        PackBrowserIndex.filter(state.packs, state.itemNames, state.organization.items, state.browser, query)
    }
    Column(Modifier.fillMaxSize()) {
        PackBrowserControls(
            state.browser,
            state.packs.map {
                (state.organization.items[it.libraryItemId] ?: PackBrowserIndex.defaults(it)).collection
            }.distinct().sorted(),
            { onIntent(PacksTabIntent.Browse(it)) },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> LoadingBox()

                state.error != null -> ActorManagerMessage(state.error, "Retry") { onIntent(PacksTabIntent.Reload) }

                state.packs.isEmpty() -> ActorManagerMessage(
                    "Build a pack of stamp-ready drawings to see it here.",
                    "New pack",
                    onRequestCreate,
                )

                filtered.isEmpty() -> EmptySearchMessage()

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.libraryItemId }) { pack ->
                        HomePackCard(
                            pack.copy(
                                displayName =
                                state.organization.items[pack.libraryItemId]?.displayName ?: pack.displayName,
                            ),
                            state.itemCounts[pack.libraryItemId] ?: 0,
                            previewFiles[pack.libraryItemId].orEmpty(),
                            PackCardActions(
                                onOpen = { onIntent(PacksTabIntent.OpenDetails(pack)) },
                                onEdit = { onEdit(pack) },
                                onShare = { onShare(pack) },
                                onDelete = { deleteCandidate = pack },
                                onFavourite = { onIntent(PacksTabIntent.ToggleFavourite(pack)) },
                                onOrganise = { organizeCandidate = pack },
                                favourite = state.organization.items[pack.libraryItemId]?.favourite == true,
                            ),
                        )
                    }
                }
            }
        }
    }
    organizeCandidate?.let { pack ->
        PackOrganizationDialog(
            pack.displayName,
            state.organization.items[pack.libraryItemId] ?: PackBrowserIndex.defaults(pack),
            { organizeCandidate = null },
            {
                onIntent(PacksTabIntent.Organise(pack, it))
                organizeCandidate = null
            },
        )
    }
    deleteCandidate?.let { pack ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete \"${pack.displayName}\"?") },
            text = {
                Text("This removes the pack from your library. Anyone you already shared it with keeps their copy.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(PacksTabIntent.Delete(pack.libraryItemId))
                    deleteCandidate =
                        null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }
    state.openDetails?.let { pack ->
        Box(Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                PackDetailsScreen(
                    state = PackDetailsState(
                        packTitle = state.organization.items[pack.libraryItemId]?.displayName ?: pack.displayName,
                        entries = state.detailsItems,
                        loading = state.detailsLoading,
                        itemFile = { entry -> detailsPreviewFile(pack.libraryItemId, entry) },
                    ),
                    onClose = { onIntent(PacksTabIntent.CloseDetails) },
                    onEdit = {
                        onIntent(PacksTabIntent.CloseDetails)
                        onEdit(pack)
                    },
                )
            }
        }
    }
}
