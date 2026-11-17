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
    val muted = Color(0xFF7C7F88)
    val dark: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val inactive = Color(0xFFB7B9C2)
    val border = Color(0xFF6B6D74)
    val accent: Color @Composable get() = MaterialTheme.colorScheme.primary

    // Canvas overlays and timeline markers, drawn over artwork, so they don't follow the theme.
    val selection = Color(0xFF4285F4)
    val playhead = Color(0xFF5796F5)
    val onionBefore = Color(0xFF92003B)
    val onionAfter = Color(0xFF00C853)

    // Light wells behind the dock's previews (grid, onion skin and brush stroke), which show
    // dark marks on paper whatever the theme.
    val previewWell = Color(0xFFF8F8F8)
    val brushPreviewWell = Color(0xFFF7F5F8)
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
}
