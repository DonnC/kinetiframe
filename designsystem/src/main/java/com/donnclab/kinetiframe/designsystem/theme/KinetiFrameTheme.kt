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

internal val BrandDarkColorScheme = darkColorScheme(
    primary = BrandMint, onPrimary = BrandLightOnPrimaryContainer,
    primaryContainer = BrandTeal, onPrimaryContainer = BrandIvory,
    secondary = BrandAmber, onSecondary = BrandLightOnSecondaryContainer,
    secondaryContainer = Color(0xFF5C410F), onSecondaryContainer = BrandLightSecondaryContainer,
    tertiary = Color(0xFFB2D1BD), onTertiary = BrandLightOnTertiaryContainer,
    tertiaryContainer = Color(0xFF2E4B3E), onTertiaryContainer = BrandLightTertiaryContainer,
    background = BrandNight, onBackground = BrandIvory,
    surface = BrandNightSurface, onSurface = BrandIvory,
    surfaceVariant = BrandNightVariant, onSurfaceVariant = Color(0xFFBFCBBF),
    outline = Color(0xFF899B8E), outlineVariant = Color(0xFF414F46),
    inverseSurface = BrandIvory, inverseOnSurface = BrandInk, inversePrimary = BrandTeal,
    surfaceTint = BrandMint,
    surfaceDim = BrandNight, surfaceBright = Color(0xFF35443C),
    surfaceContainerLowest = Color(0xFF0C1512), surfaceContainerLow = BrandNightSurface,
    surfaceContainer = Color(0xFF1D2C26), surfaceContainerHigh = BrandNightVariant,
    surfaceContainerHighest = Color(0xFF34453C),
)

internal val BrandLightColorScheme = lightColorScheme(
    primary = BrandTeal, onPrimary = BrandIvory,
    primaryContainer = BrandLightPrimaryContainer, onPrimaryContainer = BrandLightOnPrimaryContainer,
    secondary = BrandLightSecondary, onSecondary = BrandIvory,
    secondaryContainer = BrandLightSecondaryContainer, onSecondaryContainer = BrandLightOnSecondaryContainer,
    tertiary = BrandLightTertiary, onTertiary = BrandIvory,
    tertiaryContainer = BrandLightTertiaryContainer, onTertiaryContainer = BrandLightOnTertiaryContainer,
    background = BrandIvory, onBackground = BrandInk,
    surface = BrandIvory, onSurface = BrandInk,
    surfaceVariant = Color(0xFFE4E9DD), onSurfaceVariant = Color(0xFF414C44),
    outline = BrandOutline, outlineVariant = Color(0xFFC3CCC1),
    inverseSurface = BrandInk, inverseOnSurface = BrandIvory, inversePrimary = BrandMint,
    surfaceTint = BrandTeal,
    surfaceDim = Color(0xFFDBDED2), surfaceBright = Color(0xFFFFFCF4),
    surfaceContainerLowest = Color(0xFFFFFEF9), surfaceContainerLow = Color(0xFFF3F1E6),
    surfaceContainer = Color(0xFFEDEEE1), surfaceContainerHigh = Color(0xFFE6E9DD),
    surfaceContainerHighest = Color(0xFFE0E4D7),
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
