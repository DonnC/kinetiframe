package com.donnclab.kinetiframe.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// One hue: primary is the logo's carmine, secondary and tertiary are muted tones of it, and the
// surfaces are the logo's graphite and paper neutrals. Tones come from carmine's HCT palette.
internal val BrandDarkColorScheme = darkColorScheme(
    primary = Carmine70, onPrimary = Carmine20,
    primaryContainer = Carmine30, onPrimaryContainer = Carmine90,
    secondary = MutedCarmine80, onSecondary = MutedCarmine20,
    secondaryContainer = MutedCarmine30, onSecondaryContainer = Carmine90,
    tertiary = MutedCarmine80, onTertiary = MutedCarmine20,
    tertiaryContainer = MutedCarmine30, onTertiaryContainer = Carmine90,
    background = NeutralNight, onBackground = NeutralNightText,
    surface = NeutralNightSurface, onSurface = NeutralNightText,
    surfaceVariant = NeutralNightHigh, onSurfaceVariant = NeutralNightOnVariant,
    outline = NeutralNightOutline, outlineVariant = NeutralInkVariant,
    inverseSurface = BrandPaper, inverseOnSurface = BrandGraphite, inversePrimary = BrandCarmine,
    surfaceTint = Carmine70,
    surfaceDim = NeutralNight, surfaceBright = NeutralNightBright,
    surfaceContainerLowest = NeutralNightLowest, surfaceContainerLow = NeutralNightLow,
    surfaceContainer = BrandGraphite, surfaceContainerHigh = NeutralNightHigh,
    surfaceContainerHighest = NeutralNightHighest,
)

internal val BrandLightColorScheme = lightColorScheme(
    primary = BrandCarmine, onPrimary = Color.White,
    primaryContainer = Carmine90, onPrimaryContainer = Carmine10,
    secondary = MutedCarmine40, onSecondary = Color.White,
    secondaryContainer = MutedCarmine90, onSecondaryContainer = MutedCarmine10,
    tertiary = MutedCarmine40, onTertiary = Color.White,
    tertiaryContainer = MutedCarmine90, onTertiaryContainer = MutedCarmine10,
    background = BrandPaper, onBackground = BrandGraphite,
    surface = BrandPaper, onSurface = BrandGraphite,
    surfaceVariant = NeutralPaperHigh, onSurfaceVariant = NeutralInkVariant,
    outline = NeutralOutline, outlineVariant = NeutralOutlineVariant,
    inverseSurface = BrandGraphite, inverseOnSurface = BrandPaper, inversePrimary = Carmine70,
    surfaceTint = BrandCarmine,
    surfaceDim = NeutralPaperDim, surfaceBright = NeutralPaperLowest,
    surfaceContainerLowest = NeutralPaperLowest, surfaceContainerLow = NeutralPaperLow,
    surfaceContainer = NeutralPaperContainer, surfaceContainerHigh = NeutralPaperHigh,
    surfaceContainerHighest = NeutralPaperHighest,
)

@Composable
fun KinetiFrameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> BrandDarkColorScheme

        else -> BrandLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
