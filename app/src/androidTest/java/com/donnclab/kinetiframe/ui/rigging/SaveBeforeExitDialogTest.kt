package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.ui.components.dialogs.SaveBeforeExitDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SaveBeforeExitDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun discardDoesNotInvokeSaveAndCancelDoesNotExit() {
        var saved = 0
        var discarded = 0
        var cancelled = 0
        compose.setContent { MaterialTheme { SaveBeforeExitDialog({ saved++ }, { discarded++ }, { cancelled++ }) } }
        compose.onNodeWithText("Discard").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, discarded)
            assertEquals(0, saved)
        }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertEquals(1, cancelled)
            assertEquals(1, discarded)
            assertEquals(0, saved)
        }
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals(1, saved) }
    }
}
