package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.library.LibraryIntent
import com.donnclab.kinetiframe.presentation.library.catalog.AssetGridCard
import com.donnclab.kinetiframe.presentation.library.catalog.FilterChipRow
import com.donnclab.kinetiframe.presentation.library.preview.PreviewCatalog

/** Library's Projects tab: the user's saved projects, nothing else. */
@Composable
fun LibraryProjectsTab(items: List<AssetSummary>, onIntent: (LibraryIntent) -> Unit) {
    AssetGrid(items.filter { it.details.kind == AssetKind.PROJECT }, onIntent)
}

private val imageFilters = listOf(
    R.string.filter_all,
    R.string.library_filter_backgrounds,
    R.string.library_filter_overlays,
)

/** Library's Images tab: background images and static overlays, narrowed by [selectedFilter]. */
@Composable
fun LibraryImagesTab(items: List<AssetSummary>, selectedFilter: Int, onIntent: (LibraryIntent) -> Unit) {
    val wanted = when (selectedFilter) {
        1 -> setOf(AssetKind.BACKGROUND_IMAGE)
        2 -> setOf(AssetKind.FRAME)
        else -> setOf(AssetKind.BACKGROUND_IMAGE, AssetKind.FRAME)
    }
    Column {
        FilterChipRow(imageFilters, selectedFilter, onSelect = { onIntent(LibraryIntent.SelectFilter(it)) })
        AssetGrid(items.filter { it.details.kind in wanted }, onIntent)
    }
}

/** Cards carry fps/canvas-size chips and a duration, never an author or like count: there is no
 * community metadata for local assets. */
@Composable
private fun AssetGrid(items: List<AssetSummary>, onIntent: (LibraryIntent) -> Unit) {
    if (items.isEmpty()) {
        com.donnclab.kinetiframe.presentation.library.LibraryEmptyState()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(Dimens.gridColumns),
        contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
    ) {
        items(items, key = { it.id }) { item ->
            AssetGridCard(item, onClick = { onIntent(LibraryIntent.OpenDetails(item.details)) })
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryProjectsTabPreview() {
    KinetiFrameTheme { LibraryProjectsTab(PreviewCatalog.libraryProjects(), onIntent = {}) }
}
