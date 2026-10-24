package com.donnclab.kinetiframe.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.ui.catalog.AssetDetailsContext
import com.donnclab.kinetiframe.library.ui.catalog.AssetDetailsSheet
import com.donnclab.kinetiframe.library.ui.catalog.PackContentsData
import com.donnclab.kinetiframe.library.ui.catalog.PackContentsGrid
import com.donnclab.kinetiframe.library.ui.catalog.TopTabRow
import com.donnclab.kinetiframe.library.ui.catalog.TopTabs
import com.donnclab.kinetiframe.library.ui.tabs.LibraryActorsTab
import com.donnclab.kinetiframe.library.ui.tabs.LibraryAudioTab
import com.donnclab.kinetiframe.library.ui.tabs.LibraryPacksTab
import com.donnclab.kinetiframe.library.ui.tabs.LibraryProjectsTab
import com.donnclab.kinetiframe.navigation.AppBottomNavBar
import com.donnclab.kinetiframe.navigation.BottomNavDestination
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.preview.PreviewCatalog

/**
 * The Library destination's content: four tabs (Projects, Actors, Packs, Audio) over the user's
 * local collection. Cards show fps, canvas size, duration or bone counts, and the details sheet's
 * action is "Add to Current Project", shown only when a project is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryState,
    onIntent: (LibraryIntent) -> Unit,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPalettes by remember { mutableStateOf(false) }
    var previewColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Black) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            com.donnclab.kinetiframe.presentation.common.AppBrandBar()
            LibraryTopBar(state.query, onIntent, { showPalettes = true })
            TopTabRow(TopTabs.all, state.selectedTab, onSelect = { onIntent(LibraryIntent.SelectTab(it)) })
            Box(Modifier.weight(1f)) {
                LibraryTabContent(state, onIntent)
            }
            AppBottomNavBar(active = BottomNavDestination.LIBRARY, onNavigate = onNavigate)
        }

        state.openPackContents?.let { pack ->
            PackContentsGrid(
                data = PackContentsData(pack.title, state.packContents, state.packLoading, state.packError),
                onBack = { onIntent(LibraryIntent.ClosePackContents) },
                onAddToLibrary = { onIntent(LibraryIntent.AddToCurrentProject(pack.id)) },
                modifier = Modifier.fillMaxSize(),
                showAddAction = false,
                onRetry = { onIntent(LibraryIntent.OpenPackContents(pack)) },
                onEdit = { onIntent(LibraryIntent.EditOpenPack) },
            )
        }
    }

    if (showPalettes) {
        ModalBottomSheet(onDismissRequest = { showPalettes = false }) {
            Column(Modifier.padding(Dimens.spaceXl)) {
                Text("Palettes", style = MaterialTheme.typography.titleMedium)
                com.donnclab.kinetiframe.studio.ui.color.PaletteBrowser(previewColor, { previewColor = it })
            }
        }
    }

    state.openDetails?.let { details ->
        ModalBottomSheet(onDismissRequest = { onIntent(LibraryIntent.DismissDetails) }) {
            AssetDetailsSheet(
                details = details,
                context = AssetDetailsContext(
                    hasOpenProject = state.hasOpenProject,
                    openLabel = when (libraryOpenTarget(details.id)) {
                        is LibraryOpenTarget.Project -> "Open project"
                        is LibraryOpenTarget.Actor -> "Open actor"
                        null -> null
                    },
                ),
                onAction = { onIntent(LibraryIntent.AddToCurrentProject(details.id)) },
            )
        }
    }
}

@Composable
private fun LibraryTopBar(query: String, onIntent: (LibraryIntent) -> Unit, onPalettes: () -> Unit) {
    Column {
        Row(
            Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onPalettes) { Icon(Icons.Filled.Palette, "Palettes") }
                IconButton(onClick = {
                    onIntent(LibraryIntent.Reload)
                }) { Icon(Icons.Filled.Refresh, "Refresh Library") }
            }
        }
        CompactTextField(
            value = query,
            onValueChange = { onIntent(LibraryIntent.Search(it)) },
            placeholder = {
                Text("Search your Library")
            },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceXl),
        )
        Text(
            stringResource(R.string.library_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.spaceXl).padding(bottom = Dimens.spaceS),
        )
    }
}

@Composable
private fun LibraryTabContent(state: LibraryState, onIntent: (LibraryIntent) -> Unit) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }
    val query = state.query.trim()
    when (state.selectedTab) {
        TopTabs.PROJECTS -> LibraryProjectsTab(
            state.projects.filter {
                it.title.contains(query, true)
            },
            state.selectedFilter,
            onIntent,
        )

        TopTabs.ACTORS -> LibraryActorsTab(
            state.actors.filter {
                it.title.contains(query, true)
            },
            state.selectedFilter,
            onIntent,
        )

        TopTabs.PACKS -> com.donnclab.kinetiframe.library.ui.tabs.OrganizedLibraryPacksTab(state, onIntent)

        TopTabs.AUDIO -> LibraryAudioTab(
            state.audio.filter {
                it.title.contains(query, true)
            },
            state.selectedFilter,
            onIntent,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    val state = LibraryState(
        loading = false,
        projects = PreviewCatalog.libraryProjects(),
        actors = PreviewCatalog.libraryActors(),
        packs = PreviewCatalog.libraryPacks(),
        audio = PreviewCatalog.libraryAudio(),
    )
    KinetiFrameTheme { LibraryScreen(state, onIntent = {}, onNavigate = {}) }
}
