package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ClipboardPlus
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.StampAsset
import com.donnclab.kinetiframe.preview.PreviewCatalog

/** The title + stamp list [PackContentsGrid] renders -- bundled so the screen stays a
 * 4-parameter function. */
data class PackContentsData(
    val packTitle: String,
    val stamps: List<StampAsset>,
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * The pack drill-down screen: header with a back button and item count, a 4-column grid of every
 * stamp graphic inside the pack, and a sticky "Add pack to Library" bar. Shared by both
 * the Library, since tapping a pack card leads here.
 */
@Composable
fun PackContentsGrid(
    data: PackContentsData,
    onBack: () -> Unit,
    onAddToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    showAddAction: Boolean = true,
    onRetry: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
) {
    var selected by remember(data.packTitle) { mutableStateOf<StampAsset?>(null) }
    selected?.let { stamp -> StampPreviewDialog(stamp, { selected = null }) }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Column(Modifier.weight(1f)) {
                    Text(data.packTitle, style = MaterialTheme.typography.titleMedium)
                    if (!data.loading &&
                        data.error == null
                    ) {
                        Text(
                            pluralStringResource(
                                R.plurals.pack_contents_item_count,
                                data.stamps.size,
                                data.stamps.size,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                onEdit?.let { edit ->
                    IconButton(onClick = edit, enabled = !data.loading && data.error == null) {
                        Icon(Icons.Filled.Edit, "Edit pack")
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    data.loading -> CircularProgressIndicator()

                    data.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(data.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }

                    data.stamps.isEmpty() -> Text(
                        "This pack has no drawings",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
                    ) {
                        items(data.stamps, key = {
                            it.id
                        }) { stamp -> StampThumbnail(stamp, Modifier.clickable { selected = stamp }) }
                    }
                }
            }
            if (showAddAction) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    PrimaryActionButton(
                        content = ActionButtonContent(
                            stringResource(R.string.pack_add_to_library),
                            Lucide.ClipboardPlus,
                        ),
                        state = ActionButtonState.ENABLED,
                        onClick = onAddToLibrary,
                        modifier = Modifier.padding(Dimens.spaceL),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PackContentsGridPreview() {
    KinetiFrameTheme {
        PackContentsGrid(PackContentsData("Mouth Expressions", PreviewCatalog.packContents()), onBack = {
        }, onAddToLibrary = {})
    }
}
