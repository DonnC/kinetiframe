// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.CopyPlus
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Merge
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.designsystem.theme.TimelineTokens
import com.donnclab.kinetiframe.presentation.R

/** What a layer's menu can do; a null action is left out, such as Merge down on the bottom layer. */
data class LayerMenuActions(
    val onRename: () -> Unit,
    val onDuplicate: (() -> Unit)?,
    val onMergeDown: (() -> Unit)?,
    val onDelete: (() -> Unit)?,
)

/**
 * A layer's ⋮ menu (R3-27): Rename, Duplicate, Merge down and Delete. Tapping a layer only selects
 * it now; tap and long-press used to both start renaming, and there was no way to delete.
 */
@Composable
fun LayerMenu(actions: LayerMenuActions, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }, modifier = Modifier.size(TimelineTokens.layerToggle)) {
            Icon(Lucide.EllipsisVertical, stringResource(R.string.layers_menu), Modifier.size(EditorTokens.iconSmall))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            MenuItem(R.string.layers_rename, Lucide.Pencil, actions.onRename) { open = false }
            actions.onDuplicate?.let { MenuItem(R.string.layers_duplicate, Lucide.CopyPlus, it) { open = false } }
            actions.onMergeDown?.let { MenuItem(R.string.layers_merge_down, Lucide.Merge, it) { open = false } }
            actions.onDelete?.let { MenuItem(R.string.layers_delete, Lucide.Trash2, it) { open = false } }
        }
    }
}

@Composable
private fun MenuItem(@StringRes label: Int, icon: ImageVector, action: () -> Unit, close: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, null, Modifier.size(EditorTokens.icon)) },
        onClick = {
            close()
            action()
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun LayerMenuPreview() {
    KinetiFrameTheme { LayerMenu(LayerMenuActions({}, {}, null, {})) }
}
