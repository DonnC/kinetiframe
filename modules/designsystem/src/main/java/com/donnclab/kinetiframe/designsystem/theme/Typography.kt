package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.donnclab.kinetiframe.designsystem.R

/**
 * Nunito, the font the wordmark is set in. Its rounded ends match the pencil-stroke look of the
 * brand, and its large x-height and open shapes stay readable at small sizes. SIL Open Font
 * License. Each weight is its own static file: Compose caches resource fonts by ID, so weights
 * drawn from one variable file all render at whichever weight loaded first.
 */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.nunito(weight: FontWeight) = copy(fontFamily = Nunito, fontWeight = weight)

// Headings are heavier than Material's defaults: Nunito's rounded strokes read lighter than Roboto
// at the same weight, and bolder titles echo the ExtraBold wordmark.
val Typography = Typography(
    displayLarge = base.displayLarge.nunito(FontWeight.Bold),
    displayMedium = base.displayMedium.nunito(FontWeight.Bold),
    displaySmall = base.displaySmall.nunito(FontWeight.Bold),
    headlineLarge = base.headlineLarge.nunito(FontWeight.Bold),
    headlineMedium = base.headlineMedium.nunito(FontWeight.Bold),
    headlineSmall = base.headlineSmall.nunito(FontWeight.Bold),
    titleLarge = base.titleLarge.nunito(FontWeight.Bold),
    titleMedium = base.titleMedium.nunito(FontWeight.SemiBold),
    titleSmall = base.titleSmall.nunito(FontWeight.SemiBold),
    bodyLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = base.bodyMedium.nunito(FontWeight.Normal),
    bodySmall = base.bodySmall.nunito(FontWeight.Normal),
    labelLarge = base.labelLarge.nunito(FontWeight.SemiBold),
    labelMedium = base.labelMedium.nunito(FontWeight.SemiBold),
    labelSmall = base.labelSmall.nunito(FontWeight.SemiBold),
)
