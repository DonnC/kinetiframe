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
}
