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
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.library.LibraryIntent
import com.donnclab.kinetiframe.presentation.library.catalog.AssetGridCard
import com.donnclab.kinetiframe.presentation.library.catalog.FilterChipRow
import com.donnclab.kinetiframe.presentation.preview.PreviewCatalog

private val filters = listOf(
    R.string.filter_all,
    R.string.library_filter_full_projects,
    R.string.library_filter_background_images,
    R.string.library_filter_static_overlays,
)

/** Library's "Projects & Scenes" tab -- the user's own full projects, background images and
 * static overlays, narrowed by [selectedFilter]. Cards carry fps/canvas-size chips and a duration,
 * never an author or like count -- there is no community metadata for local assets. */
@Composable
fun LibraryProjectsTab(projects: List<AssetSummary>, selectedFilter: Int, onIntent: (LibraryIntent) -> Unit) {
    val kind = when (selectedFilter) {
        1 -> com.donnclab.kinetiframe.presentation.common.AssetKind.PROJECT
        2 -> com.donnclab.kinetiframe.presentation.common.AssetKind.BACKGROUND_IMAGE
        3 -> com.donnclab.kinetiframe.presentation.common.AssetKind.FRAME
        else -> null
    }
    val filtered = projects.filter { kind == null || it.details.kind == kind }
    Column {
        FilterChipRow(filters, selectedFilter, onSelect = { onIntent(LibraryIntent.SelectFilter(it)) })
        if (filtered.isEmpty()) {
            com.donnclab.kinetiframe.presentation.library.LibraryEmptyState()
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
            }) { project -> AssetGridCard(project, onClick = { onIntent(LibraryIntent.OpenDetails(project.details)) }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryProjectsTabPreview() {
    KinetiFrameTheme { LibraryProjectsTab(PreviewCatalog.libraryProjects(), selectedFilter = 0, onIntent = {}) }
}
