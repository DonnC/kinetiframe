// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LibraryTokens {
    /** Behind a pack's stamp drawings, which are dark marks on transparent paper. */
    val stampWell = Color(0xFFF1F1F1)

    /** The stack glyph on a pack card with no readable drawing, grey on the light well. */
    val stampPlaceholder = Color(0xFF9A9A9A)
    val trayWell = Color(0x11000000)

    // A local image's gradient while it loads.
    val thumbnailLoadingStart = Color(0xFFF3F0F4)
    val thumbnailLoadingEnd = Color(0xFFE5E0E8)

    // Cards for library items without an image.
    val itemCardStart = Color(0xFF677D9A)
    val itemCardEnd = Color(0xFF374C67)

    /** Start of an audio row's play button gradient, which ends in the primary color. */
    val playGradientStart = Color(0xFFFFD9CC)

    // Browser controls and grids.
    val controlHeight = 40.dp
    val filterMenuMaxHeight = 320.dp
    val pickerCell = 140.dp
    val packItemCell = 100.dp
    val tagListMaxHeight = 240.dp

    // The Colours tab.
    val colourSwatch = 48.dp
    val colourStripHeight = 28.dp
    val colourWheel = 240.dp
    val colourPreview = 96.dp

    // A stamp's preview dialog.
    val stampPreviewMaxHeight = 360.dp
    val stampPreviewHeight = 200.dp

    // The pack tray over the editor.
    val trayMaxWidth = 220.dp
    val trayMinHeight = 56.dp
    val trayMaxHeight = 260.dp
    val trayItem = 64.dp
    val trayItemThumb = 40.dp
}
