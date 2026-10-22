package com.donnclab.kinetiframe.library.ui.catalog

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * The secondary filter row shown under the top tabs (e.g. "Full Projects / Background Images /
 * Static Overlays") on every top tab except Packs, which stays one flat list so packs are quick
 * to scan.
 */
@Composable
fun FilterChipRow(filters: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        filters.forEachIndexed { index, label ->
            FilterChip(label, active = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val background = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.radiusPill),
        color = background,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                FeatureStrings.Library.FILTER_FULL_PROJECTS,
                FeatureStrings.Library.FILTER_BACKGROUND_IMAGES,
                FeatureStrings.Library.FILTER_STATIC_OVERLAYS,
            ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
