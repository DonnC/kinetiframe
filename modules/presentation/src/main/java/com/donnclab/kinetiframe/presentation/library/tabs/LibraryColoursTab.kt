// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.library.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.presentation.studio.dock.ColorPickerPanel

/**
 * Library's Colours tab: the same colour panel as the studio, so saving a colour works the same
 * everywhere. Pick a colour on the wheel or by hex and tap + to keep it in My colours; the palette
 * view creates, fills and deletes palettes of your own.
 */
@Composable
fun LibraryColoursTab() {
    var colour by rememberSaveable { mutableStateOf(Color.Black.toArgb()) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ColorPickerPanel(color = Color(colour), onColorChanged = { colour = it.toArgb() })
    }
}
