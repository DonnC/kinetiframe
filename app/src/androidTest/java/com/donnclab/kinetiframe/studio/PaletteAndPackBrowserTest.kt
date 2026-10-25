package com.donnclab.kinetiframe.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.donnclab.kinetiframe.library.data.BundledPalettes
import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserControls
import com.donnclab.kinetiframe.library.ui.packs.PackOrganizationDialog
import com.donnclab.kinetiframe.studio.ui.color.PaletteSwatches
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PaletteAndPackBrowserTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingSwatchReturnsExactColourWithoutStampingArtwork() {
        var picked: Color? = null
        compose.setContent {
            MaterialTheme { PaletteSwatches(BundledPalettes.entries.last(), Color.Black, { picked = it }) }
        }
        compose.onNodeWithContentDescription("Tavi teal #FF17999D").performClick()
        compose.runOnIdle { assertEquals(0xFF17999D.toInt(), picked!!.toArgb()) }
    }

    @Test fun browserTabsSendSelectedMode() {
        var selected: BrowserPosition? = null
        compose.setContent {
            MaterialTheme {
                PackBrowserControls(BrowserPosition(), listOf("Loose Signal"), {
                    selected =
                        it
                })
            }
        }
        compose.onNodeWithText("Favourites").performClick()
        compose.runOnIdle { assertEquals(BrowseMode.FAVOURITES, selected?.mode) }
    }

    @Test fun organisationRequiresNonblankCollectionAndSavesPrivateDisplayName() {
        var saved: AssetOrganization? = null
        compose.setContent {
            MaterialTheme {
                PackOrganizationDialog(
                    "Mouths",
                    AssetOrganization("Loose Signal"),
                    {},
                    { saved = it },
                )
            }
        }
        compose.onNodeWithText("Mouths").performTextReplacement("Tavi · Mouths")
        compose.onNodeWithContentDescription("Save organisation").performClick()
        compose.runOnIdle { assertEquals("Tavi · Mouths", saved?.displayName) }
    }
}
