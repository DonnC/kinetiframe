package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AppBottomNavBar
import com.donnclab.kinetiframe.presentation.common.BottomNavDestination
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.library.catalog.AssetDetailsContext
import com.donnclab.kinetiframe.presentation.library.catalog.AssetDetailsSheet
import com.donnclab.kinetiframe.presentation.library.catalog.PackContentsData
import com.donnclab.kinetiframe.presentation.library.catalog.PackContentsGrid
import com.donnclab.kinetiframe.presentation.library.catalog.TopTabRow
import com.donnclab.kinetiframe.presentation.library.catalog.TopTabs
import com.donnclab.kinetiframe.presentation.library.preview.PreviewCatalog
import com.donnclab.kinetiframe.presentation.library.tabs.LibraryActorsTab
import com.donnclab.kinetiframe.presentation.library.tabs.LibraryAudioTab
import com.donnclab.kinetiframe.presentation.library.tabs.LibraryPacksTab
import com.donnclab.kinetiframe.presentation.library.tabs.LibraryProjectsTab

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
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // The shared app bar, with Library's search in it: the tab bar already says where you are.
            com.donnclab.kinetiframe.presentation.common.AppBrandBar(
                middle = { LibrarySearch(state.query, onIntent) },
                actions = {
                    IconButton(onClick = { onIntent(LibraryIntent.Reload) }) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.library_refresh))
                    }
                },
            )
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

    state.openDetails?.let { details ->
        ModalBottomSheet(onDismissRequest = { onIntent(LibraryIntent.DismissDetails) }) {
            // Clear of the navigation bar, which half covered the Open button (R3-8).
            Box(Modifier.navigationBarsPadding()) {
                AssetDetailsSheet(
                    details = details,
                    context = AssetDetailsContext(
                        hasOpenProject = state.hasOpenProject,
                        openLabel = when (libraryOpenTarget(details.id)) {
                            is LibraryOpenTarget.Project -> stringResource(R.string.library_open_project)
                            is LibraryOpenTarget.Actor -> stringResource(R.string.library_open_actor)
                            null -> null
                        },
                    ),
                    onAction = { onIntent(LibraryIntent.AddToCurrentProject(details.id)) },
                )
            }
        }
    }
}

@Composable
private fun LibrarySearch(query: String, onIntent: (LibraryIntent) -> Unit) {
    CompactTextField(
        value = query,
        onValueChange = { onIntent(LibraryIntent.Search(it)) },
        placeholder = { Text(stringResource(R.string.library_search)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        // A search box, not a banner: it stops growing on a tablet.
        modifier = Modifier.widthIn(max = LibraryTokens.searchMaxWidth).fillMaxWidth(),
    )
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
        TopTabs.PROJECTS -> LibraryProjectsTab(state.projects.filter { it.title.contains(query, true) }, onIntent)

        TopTabs.IMAGES -> com.donnclab.kinetiframe.presentation.library.tabs.LibraryImagesTab(
            state.projects.filter { it.title.contains(query, true) },
            state.selectedFilter,
            onIntent,
        )

        TopTabs.COLOURS -> com.donnclab.kinetiframe.presentation.library.tabs.LibraryColoursTab()

        TopTabs.ACTORS -> LibraryActorsTab(
            state.actors.filter {
                it.title.contains(query, true)
            },
            state.selectedFilter,
            onIntent,
        )

        TopTabs.PACKS -> com.donnclab.kinetiframe.presentation.library.tabs.OrganizedLibraryPacksTab(state, onIntent)

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
