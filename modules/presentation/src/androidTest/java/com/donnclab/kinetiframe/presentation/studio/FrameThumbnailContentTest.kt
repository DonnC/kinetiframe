// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** R3-19: thumbnails show the whole canvas in its own shape instead of cropping it to the tile. */
class FrameThumbnailContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun portraitFrameInASquareTileKeepsItsShape() {
        val bounds = thumbnailBounds(Bitmap.createBitmap(90, 160, Bitmap.Config.ARGB_8888), tile = 160.dp)
        assertEquals(160f, bounds.second.value, 0.5f)
        assertEquals(90f, bounds.first.value, 0.5f)
    }

    @Test fun landscapeFrameInASquareTileKeepsItsShape() {
        val bounds = thumbnailBounds(Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888), tile = 160.dp)
        assertEquals(160f, bounds.first.value, 0.5f)
        assertEquals(90f, bounds.second.value, 0.5f)
    }

    /** The drawn frame's width and height inside a square tile. */
    private fun thumbnailBounds(bitmap: Bitmap, tile: Dp): Pair<Dp, Dp> {
        compose.setContent {
            MaterialTheme { Box(Modifier.size(tile)) { FrameThumbnailContent(bitmap, 0, false) } }
        }
        val rect = compose.onNodeWithContentDescription("Preview of frame 1").getUnclippedBoundsInRoot()
        return (rect.right - rect.left) to (rect.bottom - rect.top)
    }
}
