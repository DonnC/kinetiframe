package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.library.LibraryIntent
import com.donnclab.kinetiframe.presentation.library.LibraryState
import com.donnclab.kinetiframe.presentation.library.packs.PackBrowserControls
import com.donnclab.kinetiframe.presentation.library.packs.PackBrowserIndex

@Composable
fun OrganizedLibraryPacksTab(state: LibraryState, onIntent: (LibraryIntent) -> Unit) {
    val manifests = state.packManifests.values.toList()
    val visible = PackBrowserIndex.filter(
        manifests,
        state.packItemNames,
        state.organization.items,
        state.packBrowser,
        state.query,
    )
        .map { PackBrowserIndex.key(it) }
    Column(Modifier.fillMaxSize()) {
        PackBrowserControls(
            state.packBrowser,
            manifests.map {
                (state.organization.items[PackBrowserIndex.key(it)] ?: PackBrowserIndex.defaults(it)).collection
            }.distinct().sorted(),
            { onIntent(LibraryIntent.BrowsePacks(it)) },
        )
        Box(Modifier.weight(1f)) {
            val byKey = state.packs.associateBy { card ->
                state.packManifests[card.id]?.let(PackBrowserIndex::key)
                    ?: card.id
            }
            val ordered = if (manifests.isEmpty()) {
                state.packs.filter {
                    it.title.contains(state.query, true)
                }
            } else {
                visible.mapNotNull(byKey::get)
            }
            LibraryPacksTab(
                ordered.map { card ->
                    val manifest = state.packManifests[card.id]
                    val alias = manifest?.let { state.organization.items[PackBrowserIndex.key(it)]?.displayName }
                    if (alias == null) card else card.copy(title = alias, details = card.details.copy(title = alias))
                },
                onIntent,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrganizedLibraryPacksTabPreview() {
    KinetiFrameTheme { OrganizedLibraryPacksTab(LibraryState(loading = false), {}) }
}
