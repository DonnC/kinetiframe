package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HomeTokens {
    val cardCorner = 16.dp
    val chipCorner = 8.dp
    val chipBg = Color(0x8C141210)

    /** Room under the last card for the bottom bar and the New button above it. */
    val listBottomReserve = 160.dp

    // Card overlays.
    val badgePaddingHorizontal = 7.dp
    val placeholderIcon = 40.dp

    // The create sheet's option tiles.
    val createOptionTile = 46.dp

    /** Backgrounds for cards without a thumbnail; a card picks one from its ID. */
    val placeholderTints = listOf(
        Color(0xFFFFB199),
        Color(0xFF9FCBEF),
        Color(0xFF8FCB8A),
        Color(0xFFB77FE0),
        Color(0xFFE8C24C),
        Color(0xFFF2879F),
    )
}
