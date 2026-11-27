// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.studio.color.PaletteViewModel
import com.donnclab.kinetiframe.presentation.studio.dock.ColorPicker
import com.donnclab.kinetiframe.ui.appViewModel

/** How many recent and saved colours the row shows beside the current one. */
private const val QUICK_COLOURS = 10

/**
 * The workspace's colour in one row: the current colour, which opens the full picker, then the
 * recent and saved colours to tap. The phone's quick swatches with a hex field took a third of the
 * tool panel's height, and the panel must fit without scrolling.
 */
@Composable
internal fun WorkspaceColourRow(colour: Color, onPick: (Color) -> Unit) {
    val palettes: PaletteViewModel = appViewModel(key = "palette-browser")
    val state by palettes.state.collectAsStateWithLifecycle()
    val quick = (state.recent + state.myColours?.swatches?.map { it.argb }.orEmpty()).distinct().take(QUICK_COLOURS)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        ColorPicker(colour, onPick)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            items(quick, key = { it }) { argb ->
                val swatch = Color(argb.toInt())
                val hex = PaletteEdits.hex(argb)
                Box(
                    Modifier.size(EditorTokens.workspaceSwatch).background(swatch, CircleShape)
                        .border(Dimens.borderThin, MaterialTheme.colorScheme.outline, CircleShape)
                        .semantics { contentDescription = hex }
                        .clickable { onPick(swatch) },
                )
            }
        }
    }
}
