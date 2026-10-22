package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Grid2x2
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** One entry in a [TopTabRow] -- deliberately just an icon+label pair with no selection state of
 *  its own, so the row above decides which index is active. */
data class TopTabDef(val icon: ImageVector, val label: String)

/** The four top-level category tabs the Library uses (Projects / Actors / Packs /
 *  Audio), sharing one definition so the two screens can never drift out of sync with each other. */
object TopTabs {
    val all = listOf(
        TopTabDef(Lucide.Folder, FeatureStrings.Common.TAB_PROJECTS),
        TopTabDef(Lucide.Drama, FeatureStrings.Common.TAB_ACTORS),
        TopTabDef(Lucide.Grid2x2, FeatureStrings.Common.TAB_PACKS),
        TopTabDef(Lucide.Music, FeatureStrings.Common.TAB_AUDIO),
    )
    const val PROJECTS = 0
    const val ACTORS = 1
    const val PACKS = 2
    const val AUDIO = 3
}

/** Icon-over-label row of top category tabs, with a colored underline on the selected one. */
@Composable
fun TopTabRow(tabs: List<TopTabDef>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                TopTabItem(tab, active = index == selectedIndex, onClick = {
                    onSelect(index)
                }, modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun TopTabItem(tab: TopTabDef, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val underlineColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier.selectable(selected = active, role = Role.Tab, onClick = onClick).padding(vertical = Dimens.spaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(Dimens.iconM))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
        Box(Modifier.fillMaxWidth().height(2.dp).background(underlineColor))
    }
}

@Preview(showBackground = true)
@Composable
private fun TopTabRowPreview() {
    KinetiFrameTheme { TopTabRow(TopTabs.all, selectedIndex = TopTabs.PACKS, onSelect = {}) }
}
