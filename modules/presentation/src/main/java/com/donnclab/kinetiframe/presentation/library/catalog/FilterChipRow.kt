package com.donnclab.kinetiframe.presentation.library.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/**
 * The secondary filter row shown under the top tabs (e.g. "Full Projects / Background Images /
 * Static Overlays") on every top tab except Packs, which stays one flat list so packs are quick
 * to scan.
 *
 * [filters] starts with the unfiltered entry ("All"), which has no chip of its own: with no chip
 * selected everything shows. Tapping a chip filters by it, and tapping it again clears the filter.
 */
@Composable
fun FilterChipRow(filters: List<Int>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        filters.forEachIndexed { index, label ->
            if (index == UNFILTERED) return@forEachIndexed
            val active = index == selectedIndex
            FilterChip(stringResource(label), active, onClick = { onSelect(if (active) UNFILTERED else index) })
        }
    }
}

private const val UNFILTERED = 0

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val background = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.radiusPill),
        color = background,
        border = if (active) null else BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceS),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterChipRowPreview() {
    KinetiFrameTheme {
        FilterChipRow(
            filters = listOf(
                R.string.filter_all,
                R.string.library_filter_full_projects,
                R.string.library_filter_background_images,
                R.string.library_filter_static_overlays,
            ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
