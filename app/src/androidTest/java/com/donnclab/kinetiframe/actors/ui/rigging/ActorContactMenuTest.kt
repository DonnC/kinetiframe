package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.actors.ui.rigging.ActorContactMenu
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActorContactMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pinActionRequiresExplicitMenuChoice() {
        var toggles = 0
        compose.setContent { KinetiFrameTheme { ActorContactMenu(true, false) { toggles++ } } }
        compose.onNodeWithContentDescription("Joint contact").performClick()
        assertEquals(0, toggles)
        compose.onNodeWithText("Pin joint from this key").performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
    }

    @Test fun contactUnavailableWithoutCompatibleChain() {
        compose.setContent { KinetiFrameTheme { ActorContactMenu(false, false, {}) } }
        compose.onNodeWithContentDescription("Joint contact").assertIsNotEnabled()
    }
}
