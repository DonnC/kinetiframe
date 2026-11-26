package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object EditorTokens {
    val icon = 18.dp
    val iconSmall = 15.dp
    val control = 32.dp
    val row = 54.dp
    val field = 34.dp
    val corner = 8.dp
    val muted = NeutralOutline
    val dark: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val inactive = Color(0xFFBFBBB5)
    val border = Color(0xFF6E6A65)
    val accent: Color @Composable get() = MaterialTheme.colorScheme.primary

    // Canvas overlays and timeline markers, drawn over artwork, so they don't follow the theme.
    // Selection and the playhead use the brand carmine. Onion skins follow the animator's
    // convention: earlier frames carmine, later frames blue, a pair red-green colour blindness
    // keeps apart. That blue is the one colour outside the brand (see OnionNextBlue).
    val selection = BrandCarmine
    val playhead = BrandCarmine
    val onionBefore = BrandCarmine
    val onionAfter = OnionNextBlue

    // Light wells behind the dock's previews (grid, onion skin and brush stroke), which show
    // dark marks on paper whatever the theme.
    val previewWell = Color(0xFFF8F8F8)
    val brushPreviewWell = BrandPaper

    /** The canvas's paper behind frame thumbnails, so a frame looks as it does on the canvas. */
    val framePaper = Color.White
    val hairline = Color(0xFFE0E0E0)

    // The tool dock: its rail, whose height depends on the studio mode, and its menus.
    val dockRailWidth = 48.dp
    val dockRailHeightPuppetStudio = 300.dp
    val dockRailHeightWithActor = 220.dp
    val dockRailHeight = 180.dp
    val dockCorner = 24.dp
    val dockRailCorner = 28.dp
    val dockMenuWidth = 200.dp
    val dockGridMenuWidth = 240.dp

    /** How far a tool's menu opens to the right of the dock. */
    val dockMenuOffset = 52.dp

    // The dock's option panels.
    val textPanelWidth = 220.dp
    val confirmIcon = 22.dp
    val closeButton = 24.dp
    val resetButton = 28.dp
    val panelAction = 40.dp
    val sectionGap = 24.dp
    val labelGap = 6.dp
    val previewGap = 10.dp
    val previewCorner = 10.dp
    val previewHeight = 80.dp
    val brushPreview = 48.dp
    val brushMenuPreview = 44.dp

    // Color controls.
    val colorDot = 24.dp
    val swatch = 22.dp
    val swatchCorner = 5.dp
    val colorSliderHeight = 16.dp
    val colorWheel = 160.dp
    val colorWheelLandscape = 120.dp
    val palettePanelWidth = 220.dp
    val paletteSwatch = 44.dp
    val swatchBorderSelected = 3.dp
    val selectionDot = 22.dp
    val selectionDotIcon = 14.dp

    // The screen colour picker's loupe.
    val loupeRadius = 24.dp
    val loupeRingStroke = 4.dp
    val loupeInnerStroke = 1.dp
    val loupeCross = 8.dp
    val loupeCrossStroke = 2.dp
    val loupeSwatch = 32.dp

    // Editor shell, popups and hints.
    val topBarHeight = 48.dp
    val popupWidth = 340.dp
    val popupHeight = 330.dp
    val popupNarrowWidth = 260.dp
    val popupMediumWidth = 280.dp
    val canvasHintStartInset = 64.dp
    val actorHintStartInset = 96.dp

    /** Keeps a canvas hint clear of the floating image button at the canvas's top end. */
    val canvasHintEndInset = 72.dp
    val transformPanelTop = 72.dp
    val cameraRangeHeight = 14.dp

    // Canvas overlays.
    val eraserDot = 2.dp
    val eraserDotCore = 1.dp
    val pivotRadius = 14.dp
    val pivotHitRadius = 44.dp

    // The frame studio grid.
    val frameStudioCorner = 22.dp
    val frameStudioCell = 150.dp
    val frameStudioCardThumb = 150.dp
    val frameStudioArrow = 10.dp
}
