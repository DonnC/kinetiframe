package com.donnclab.kinetiframe.library.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.common.ui.AssetKind
import com.donnclab.kinetiframe.common.ui.AssetSummary
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.ui.LibraryIntent
import com.donnclab.kinetiframe.library.ui.catalog.AssetGridCard
import com.donnclab.kinetiframe.library.ui.catalog.FilterChipRow
import com.donnclab.kinetiframe.preview.PreviewCatalog

private val filters = listOf("All", FeatureStrings.Library.FILTER_RIGS, FeatureStrings.Library.FILTER_PERFORMANCES)

/** Library's "Actors & Actions" tab -- the user's own rigs and performances, narrowed by
 * [selectedFilter]. */
@Composable
fun LibraryActorsTab(actors: List<AssetSummary>, selectedFilter: Int, onIntent: (LibraryIntent) -> Unit) {
    val wantedKind = when (selectedFilter) {
        1 -> AssetKind.ACTOR_RIG
        2 -> AssetKind.ACTOR_PERFORMANCE
        else -> null
    }
    val filtered = actors.filter { wantedKind == null || it.details.kind == wantedKind }
    Column {
        FilterChipRow(filters, selectedFilter, onSelect = { onIntent(LibraryIntent.SelectFilter(it)) })
        if (filtered.isEmpty()) {
            com.donnclab.kinetiframe.library.ui.LibraryEmptyState()
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(Dimens.gridColumns),
            contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
            horizontalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
        ) {
            items(filtered, key = {
                it.id
            }) { actor -> AssetGridCard(actor, onClick = { onIntent(LibraryIntent.OpenDetails(actor.details)) }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryActorsTabPreview() {
    KinetiFrameTheme { LibraryActorsTab(PreviewCatalog.libraryActors(), selectedFilter = 0, onIntent = {}) }
}
