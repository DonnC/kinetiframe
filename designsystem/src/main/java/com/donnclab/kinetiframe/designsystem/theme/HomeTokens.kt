package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HomeTokens {
    val cardCorner = 16.dp
    val chipCorner = 8.dp
    val chipBg = Color(0x8C141210)
    val cardScrim = Color(0xB8141210)
    val fabSize = 60.dp
    val fabRing = 4.dp

    /** Room under the last card for the + button. */
    val listBottomReserve = 104.dp

    // Card overlays.
    val badgePaddingHorizontal = 7.dp
    val cardTitleTop = 26.dp
    val cardMenuReserve = 40.dp
    val cardFooterReserve = 40.dp
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
