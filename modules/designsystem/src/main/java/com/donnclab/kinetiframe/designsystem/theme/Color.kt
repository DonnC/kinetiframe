package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color

// One brand colour, from the logo: carmine, the animator's key-pose pencil. Every other interface
// colour is a tone of it or one of the logo's two neutrals, graphite and paper. A single accent
// stays meaningful because it is scarce. XML equivalents are checked by BrandResourcesTest.
val BrandCarmine = Color(0xFFC9372C)
val BrandGraphite = Color(0xFF22201E)
val BrandPaper = Color(0xFFF6F3EC)

// Carmine's tonal palette (Material HCT, hue 26, full chroma), named by tone.
val Carmine10 = Color(0xFF410001)
val Carmine20 = Color(0xFF690003)
val Carmine30 = Color(0xFF910909)
val Carmine70 = Color(0xFFFF8A7B)
val Carmine90 = Color(0xFFFFDAD5)

// Muted carmine: the same hue at low chroma, for secondary roles that shouldn't compete with
// the primary action.
val MutedCarmine10 = Color(0xFF2C1512)
val MutedCarmine20 = Color(0xFF442926)
val MutedCarmine30 = Color(0xFF5D3F3B)
val MutedCarmine40 = Color(0xFF775652)
val MutedCarmine80 = Color(0xFFE7BDB7)
val MutedCarmine90 = Color(0xFFF5DDDA)

// Neutral greys between graphite and paper, too low in colour to shift what an artist sees.
val NeutralPaperLowest = Color(0xFFFDFCF9)
val NeutralPaperLow = Color(0xFFF1EEE7)
val NeutralPaperContainer = Color(0xFFECE9E2)
val NeutralPaperHigh = Color(0xFFE6E3DC)
val NeutralPaperHighest = Color(0xFFE0DDD6)
val NeutralPaperDim = Color(0xFFD9D6CF)
val NeutralInkVariant = Color(0xFF4A4743)
val NeutralOutline = Color(0xFF7A7671)
val NeutralOutlineVariant = Color(0xFFCBC7C0)
val NeutralNight = Color(0xFF161514)
val NeutralNightLowest = Color(0xFF0F0E0D)
val NeutralNightSurface = Color(0xFF1A1918)
val NeutralNightLow = Color(0xFF1D1C1A)
val NeutralNightHigh = Color(0xFF2C2A28)
val NeutralNightHighest = Color(0xFF373532)
val NeutralNightBright = Color(0xFF3C3936)
val NeutralNightOnVariant = Color(0xFFC9C5BF)
val NeutralNightOutline = Color(0xFF948F89)
val NeutralNightText = Color(0xFFEDE9E2)

// The one colour outside the brand: the canvas's next-frame onion skin. It is a drawing aid, not
// interface colour; animators mark later frames in blue, and it stays distinct from carmine with
// red-green colour blindness. Users can change it in the onion skin settings.
val OnionNextBlue = Color(0xFF2E6BD1)

val AppPrimary = BrandCarmine

// Disabled action-button state (e.g. "Add to Current Project" with no project open).
val DisabledSurface = Color(0xFFEFEDE8)
val DisabledContent = Color(0xFFA6A29B)
