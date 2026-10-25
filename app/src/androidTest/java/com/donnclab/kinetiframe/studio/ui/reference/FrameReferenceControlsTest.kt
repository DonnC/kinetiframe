package com.donnclab.kinetiframe.studio.ui.reference

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.cache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.studio.ui.actorstudio.ActorReferenceControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class FrameReferenceControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun frameSwitchRestoresGuideAndRemoveAffectsOnlyItsOwnFrame() {
        val cache = FrameReferenceSession(64L * 1024 * 1024)
        var frame by mutableIntStateOf(0)
        val first = cache.frame(0)
        val second = cache.frame(1)
        val image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        cache.replace(first, image, 100, 100)
        first.editing = false
        compose.setContent {
            MaterialTheme {
                val session = cache.frame(frame)
                key(session) { ActorReferenceControls(session, 100, 100) }
            }
        }
        compose.runOnIdle {
            frame = 1
            assertNull(second.bitmap)
        }
        compose.runOnIdle {
            frame = 0
            assertSame(image, cache.frame(frame).bitmap)
        }
        compose.onNodeWithContentDescription("Reference image layer (temporary)").performClick()
        compose.onNodeWithText("Reference · not saved or exported").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove reference").performClick()
        compose.runOnIdle {
            assertNull(first.bitmap)
            assertNull(second.bitmap)
            assertFalse(image.isRecycled)
        }
    }

    @Test fun referencePaletteTogglesVisibilityAndProportionsWithoutTextRows() {
        val session = com.donnclab.kinetiframe.studio.ui.actorstudio.ActorReferenceSession()
        session.replace(Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888), 100, 100)
        session.editing = false
        compose.setContent { MaterialTheme { ActorReferenceControls(session, 100, 100) } }
        compose.onNodeWithContentDescription("Reference image layer (temporary)").performClick()
        compose.onNodeWithText("Import image").assertDoesNotExist()
        compose.onNodeWithContentDescription("Reference visibility").performClick()
        compose.runOnIdle { assertFalse(session.visible) }
        val initialLock = session.lockAspectRatio
        compose.onNodeWithContentDescription("Lock proportions").performClick()
        compose.runOnIdle { assertEquals(!initialLock, session.lockAspectRatio) }
    }

    @Test fun clearInvalidatesImportsAndDoesNotRecycleDisplayedBitmap() {
        val cache = FrameReferenceSession(64L * 1024 * 1024)
        val session = cache.frame(0)
        val image = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val token = session.importGeneration
        cache.replace(session, image, 100, 100)
        cache.clear()
        assertNotEquals(token, session.importGeneration)
        assertNull(session.bitmap)
        assertFalse(cache.replace(session, image, 100, 100))
        assertFalse(image.isRecycled)
    }
}
