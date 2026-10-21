package com.donnclab.kinetiframe.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.data.library.AssetCategory
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.components.studio.StudioFrameCard
import com.donnclab.kinetiframe.ui.components.timeline.FrameCard
import com.donnclab.kinetiframe.ui.packs.PackBrowserControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import android.graphics.Color as AndroidColor

class BrowserAndFrameCardsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filtersUseMenusAndKeepCategoryWhenChangingScope() {
        var position by mutableStateOf(BrowserPosition(BrowseMode.COLLECTIONS))
        compose.setContent {
            KinetiFrameTheme {
                PackBrowserControls(position, listOf("Loose Signal", "General Essentials"), { position = it })
            }
        }
        compose.onNodeWithText("Loose Signal").assertDoesNotExist()
        compose.onNodeWithText("All collections").performClick()
        compose.onNodeWithText("Loose Signal").performClick()
        assertEquals("Loose Signal", position.collection)
        compose.onNodeWithText("All categories").performClick()
        compose.onNodeWithText("Expressions").performClick()
        assertEquals(AssetCategory.EXPRESSIONS, position.category)
        compose.onNodeWithText("All").performClick()
        assertNull(position.collection)
        assertEquals(AssetCategory.EXPRESSIONS, position.category)
        compose.onNodeWithText("Loose Signal").assertDoesNotExist()
        compose.onNodeWithText("Expressions").performClick()
        compose.onNodeWithText("All categories").performClick()
        assertNull(position.category)
    }

    private fun landscapeBitmap() = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888).apply {
        eraseColor(AndroidColor.RED)
    }

    @Test fun viewerThumbnailFillsCardAndEyeDoesNotSelectFrame() {
        val bitmap = landscapeBitmap()
        var selections = 0
        var opens = 0
        try {
            compose.setContent {
                KinetiFrameTheme {
                    StudioFrameCard(0, bitmap, false, Modifier.size(190.dp, 150.dp), { selections++ }, { opens++ })
                }
            }
            val pixels = compose.onNodeWithContentDescription("Frame 1 preview").captureToImage().toPixelMap()
            assertTrue(pixels[pixels.width / 2, pixels.height * 9 / 10].red > .95f)
            assertTrue(pixels[pixels.width / 2, pixels.height * 9 / 10].green < .05f)
            compose.onNodeWithText("1").assertExists()
            compose.onNodeWithContentDescription("Open frame").performClick()
            assertEquals(1, opens)
            assertEquals(0, selections)
        } finally {
            compose.runOnIdle { bitmap.recycle() }
        }
    }

    @Test fun timelineThumbnailFillsCellBelowNumberOverlay() {
        val bitmap = landscapeBitmap()
        try {
            compose.setContent { KinetiFrameTheme { FrameCard(0, false, bitmap, onClick = {}) } }
            val pixels = compose.onNodeWithContentDescription("Frame 1 preview").captureToImage().toPixelMap()
            val bottom = pixels[pixels.width / 2, pixels.height * 4 / 5]
            assertTrue(bottom.red > .95f && bottom.green < .05f)
            compose.onNodeWithText("1").assertExists()
        } finally {
            compose.runOnIdle { bitmap.recycle() }
        }
    }
}
