package com.donnclab.kinetiframe.library.ui.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.library.ui.catalog.ActionButtonContent
import com.donnclab.kinetiframe.library.ui.catalog.ActionButtonState
import com.donnclab.kinetiframe.library.ui.catalog.PrimaryActionButton
import com.donnclab.kinetiframe.presentation.R
import java.io.File

/**
 * Everything the saved-pack drill-down renders, grouped so the screen keeps a 4-parameter
 * signature instead of taking a title, a list, a file resolver and a loading flag separately.
 *
 * [itemFile] is a resolver rather than a pre-built list of files so the screen never holds copies
 * of the pack's drawings -- it asks for each path only as that tile is composed, and the pack's
 * PNGs stay read-in-place on disk (see LibraryRepository.packItemFile).
 */
data class PackDetailsState(
    val packTitle: String,
    val entries: List<PackItemEntry>,
    val loading: Boolean,
    val itemFile: (PackItemEntry) -> File,
)

/**
 * The saved-pack drill-down for Home's Packs tab: every drawing the pack actually contains, with
 * the real name given to it, over an "Edit Pack" action that reopens it in the Pack Creator.
 *
 * Distinct from the Library's `PackContentsGrid`. The action here is Edit rather than "Add pack to Library" because a pack reached from
 * this screen is already in the user's library -- re-adding it would be a no-op.
 */
@Composable
fun PackDetailsScreen(state: PackDetailsState, onClose: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            IconButton(onClick = onClose) { Icon(Lucide.X, contentDescription = stringResource(R.string.action_close)) }
            Column {
                Text(state.packTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.pack_item_count, state.entries.size, state.entries.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.loading && state.entries.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(PACK_GRID_COLUMNS),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            ) {
                items(state.entries, key = { it.file }) { entry ->
                    PackItemThumbnail(entry, state.itemFile(entry))
                }
            }
        }
        Surface(tonalElevation = Dimens.elevationS, modifier = Modifier.fillMaxWidth()) {
            PrimaryActionButton(
                content = ActionButtonContent(stringResource(R.string.pack_edit_pack), Lucide.Pen),
                state = ActionButtonState.ENABLED,
                onClick = onEdit,
                modifier = Modifier.padding(Dimens.spaceL),
            )
        }
    }
}

/** Four across matches the stamp density of the Library pack grids. */
private const val PACK_GRID_COLUMNS = 4

@Preview(name = "Pack details -- light", showBackground = true)
@Preview(
    name = "Pack details -- dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PackDetailsScreenPreview() {
    KinetiFrameTheme {
        PackDetailsScreen(
            state = PackDetailsState(
                packTitle = "Mouth Expressions",
                entries = listOf(
                    PackItemEntry(file = "frame_001.png", index = 0, name = "Smile"),
                    PackItemEntry(file = "frame_002.png", index = 1, name = "Frown"),
                ),
                loading = false,
                itemFile = { File("") },
            ),
            onClose = {},
            onEdit = {},
        )
    }
}
