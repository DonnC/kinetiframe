package com.donnclab.kinetiframe.ui.library.tabs

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
import com.donnclab.kinetiframe.ui.common.model.AssetSummary
import com.donnclab.kinetiframe.ui.common.model.PreviewCatalog
import com.donnclab.kinetiframe.ui.common.strings.FeatureStrings
import com.donnclab.kinetiframe.ui.components.catalog.AssetGridCard
import com.donnclab.kinetiframe.ui.components.catalog.FilterChipRow
import com.donnclab.kinetiframe.ui.library.LibraryIntent

private val filters = listOf(
    "All",
    FeatureStrings.Library.FILTER_FULL_PROJECTS,
    FeatureStrings.Library.FILTER_BACKGROUND_IMAGES,
    FeatureStrings.Library.FILTER_STATIC_OVERLAYS,
)

/** Library's "Projects & Scenes" tab -- the user's own full projects, background images and
 * static overlays, narrowed by [selectedFilter]. Cards carry fps/canvas-size chips and a duration,
 * never an author or like count -- there is no community metadata for local assets. */
@Composable
fun LibraryProjectsTab(projects: List<AssetSummary>, selectedFilter: Int, onIntent: (LibraryIntent) -> Unit) {
    val kind = when (selectedFilter) {
        1 -> com.donnclab.kinetiframe.ui.common.model.AssetKind.PROJECT
        2 -> com.donnclab.kinetiframe.ui.common.model.AssetKind.BACKGROUND_IMAGE
        3 -> com.donnclab.kinetiframe.ui.common.model.AssetKind.FRAME
        else -> null
    }
    val filtered = projects.filter { kind == null || it.details.kind == kind }
    Column {
        FilterChipRow(filters, selectedFilter, onSelect = { onIntent(LibraryIntent.SelectFilter(it)) })
        if (filtered.isEmpty()) {
            com.donnclab.kinetiframe.ui.library.LibraryEmptyState()
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
