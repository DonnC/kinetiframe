package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ActorStudioTokens {
    val panelSurface = BrandNightSurface
    val panelSurfaceVariant = BrandNightVariant
    val panelDivider = Color(0xFF414F46)
    val onPanel = BrandIvory
    val onPanelMuted = Color(0xFFBFCBBF)

    // This panel is always dark, even in the light app theme.
    val accent = BrandMint

    /** Content on [accent], such as an enabled apply icon. */
    val onAccent = Color(0xFF0B1416)

    /** The sketch editor's binding region and its outline. */
    val sketchBind = Color(0xFF32B6AC)
    val partRailIdle = Color(0xFFF1EBF3)
    val presetIconStroke = Color(0xFF172B36)
    val panelWidth = 320.dp
    val panelMinMargin = 24.dp
}
