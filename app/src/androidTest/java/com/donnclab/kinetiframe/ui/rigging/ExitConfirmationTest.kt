package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.ui.components.navigation.ExitConfirmationDialog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExitConfirmationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stayDoesNotExit() {
        var dismissed = false
        var exited = false
        compose.setContent { MaterialTheme { ExitConfirmationDialog({ dismissed = true }, { exited = true }) } }
        compose.onNodeWithText("Stay").performClick()
        compose.runOnIdle {
            assertTrue(dismissed)
            assertFalse(exited)
        }
    }

    @Test fun exitRequiresConfirmation() {
        var exited = false
        compose.setContent { MaterialTheme { ExitConfirmationDialog({}, { exited = true }) } }
        compose.runOnIdle { assertFalse(exited) }
        compose.onNodeWithText("Exit").performClick()
        compose.runOnIdle { assertTrue(exited) }
    }
}
