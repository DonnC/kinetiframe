package com.donnclab.kinetiframe.library.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.common.ui.AssetSummary
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.ui.LibraryIntent
import com.donnclab.kinetiframe.library.ui.catalog.FlatListNote
import com.donnclab.kinetiframe.library.ui.catalog.PackGridCard
import com.donnclab.kinetiframe.preview.PreviewCatalog

/** Library's "Component Packs" tab -- deliberately flat, one continuous scroll of every pack the
 * user has saved. */
@Composable
fun LibraryPacksTab(packs: List<AssetSummary>, onIntent: (LibraryIntent) -> Unit) {
    Column {
        if (packs.isEmpty()) {
            com.donnclab.kinetiframe.library.ui.LibraryEmptyState()
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(Dimens.gridColumns),
            contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
            horizontalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.gridColumnGap),
        ) {
            items(packs, key = {
                it.id
            }) { pack -> PackGridCard(pack, onClick = { onIntent(LibraryIntent.OpenPackContents(pack)) }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryPacksTabPreview() {
    KinetiFrameTheme { LibraryPacksTab(PreviewCatalog.libraryPacks(), onIntent = {}) }
}
