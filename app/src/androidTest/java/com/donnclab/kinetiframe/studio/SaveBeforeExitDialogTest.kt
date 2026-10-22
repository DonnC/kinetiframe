package com.donnclab.kinetiframe.studio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.ui.dialogs.SaveBeforeExitDialog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SaveBeforeExitDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelNeverSavesOrNavigates() {
        var saved = false
        var discarded = false
        var cancelled = false
        compose.setContent {
            KinetiFrameTheme {
                SaveBeforeExitDialog({ saved = true }, { discarded = true }, {
                    cancelled =
                        true
                })
            }
        }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertTrue(cancelled)
            assertFalse(saved)
            assertFalse(discarded)
        }
    }

    @Test fun projectPromptDoesNotPromisePixelRollback() {
        compose.setContent { KinetiFrameTheme { SaveBeforeExitDialog({}, null, {}) } }
        compose.onNodeWithText("Save").assertExists()
        compose.onNodeWithText("Cancel").assertExists()
        compose.onNodeWithText("Discard").assertDoesNotExist()
    }

    @Test fun saveDoesNotInvokeDiscardNavigation() {
        var saved = false
        var discarded = false
        compose.setContent { KinetiFrameTheme { SaveBeforeExitDialog({ saved = true }, { discarded = true }, {}) } }
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertTrue(saved)
            assertFalse(discarded)
        }
    }
}
