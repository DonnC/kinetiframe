package com.donnclab.kinetiframe.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrandThemeTest {
    @Test
    fun `light and dark text roles meet minimum normal text contrast`() {
        listOf(BrandLightColorScheme, BrandDarkColorScheme).forEach { scheme ->
            listOf(
                scheme.primary to scheme.onPrimary,
                scheme.primaryContainer to scheme.onPrimaryContainer,
                scheme.secondary to scheme.onSecondary,
                scheme.secondaryContainer to scheme.onSecondaryContainer,
                scheme.tertiary to scheme.onTertiary,
                scheme.tertiaryContainer to scheme.onTertiaryContainer,
                scheme.background to scheme.onBackground,
                scheme.surface to scheme.onSurface,
                scheme.surfaceVariant to scheme.onSurfaceVariant,
                scheme.surfaceContainerHighest to scheme.onSurface,
            ).forEach { (background, foreground) ->
                assertTrue(
                    contrast(background, foreground) >= 4.5,
                    "Insufficient text contrast: $background / $foreground",
                )
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance().toDouble()
        val second = b.luminance().toDouble()
        return (maxOf(first, second) + .05) / (minOf(first, second) + .05)
    }
}
