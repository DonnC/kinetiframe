package com.donnclab.kinetiframe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.engine.rigging.model.ActorView
import com.donnclab.kinetiframe.ui.actors.ActorViewMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActorViewMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test fun authoredViewSelectionAndCreationAreSeparateActions() {
        var selected: ActorView? = null
        var created: ActorView? = null
        compose.setContent {
            MaterialTheme {
                ActorViewMenu(
                    ActorView.FRONT,
                    setOf(ActorView.FRONT, ActorView.PROFILE),
                    false,
                    true,
                    { selected = it },
                    { created = it },
                )
            }
        }
        compose.onNodeWithContentDescription("Actor views: Front").performClick()
        compose.onNodeWithText("Profile").performClick()
        compose.runOnIdle {
            assertEquals(ActorView.PROFILE, selected)
            assertEquals(null, created)
        }
        compose.onNodeWithContentDescription("Actor views: Front").performClick()
        compose.onNodeWithText("Add view").performClick()
        compose.onNodeWithText("Back").performClick()
        compose.runOnIdle { assertEquals(ActorView.BACK, created) }
    }

    @Test fun existingPerformanceCannotSwitchItsPinnedView() {
        compose.setContent {
            MaterialTheme {
                ActorViewMenu(ActorView.PROFILE, setOf(ActorView.FRONT, ActorView.PROFILE), true, false, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Actor views: Profile").performClick()
        compose.onNodeWithText("Front").assertIsNotEnabled()
        compose.onNodeWithText("Add view").assertDoesNotExist()
    }
}
