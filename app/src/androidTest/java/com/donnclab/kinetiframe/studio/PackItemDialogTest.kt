package com.donnclab.kinetiframe.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.presentation.library.packs.PackItemDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PackItemDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancellingNewItemDoesNotCommitMetadata() {
        var saved: PackFrameTag? = null
        var cancelled = false
        compose.setContent {
            MaterialTheme { PackItemDialog(PackFrameTag(), true, { cancelled = true }, { saved = it }) }
        }
        compose.onNodeWithContentDescription("Confirm item").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.runOnIdle {
            assertTrue(cancelled)
            assertNull(saved)
        }
    }

    @Test fun editPrefillsMetadataAndCommitsRenamedItem() {
        var saved: PackFrameTag? = null
        compose.setContent {
            MaterialTheme { PackItemDialog(PackFrameTag("Smile", "Small grin"), false, {}, { saved = it }) }
        }
        compose.onNodeWithText("Smile").performTextReplacement(" Laugh ")
        compose.onNodeWithContentDescription("Confirm item").performClick()
        compose.runOnIdle { assertEquals(PackFrameTag("Laugh", "Small grin"), saved) }
    }
}
