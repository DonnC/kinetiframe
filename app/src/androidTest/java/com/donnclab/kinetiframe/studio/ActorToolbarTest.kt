package com.donnclab.kinetiframe.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.PuppetTopBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActorToolbarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactBarKeepsBackAndSaveAndMovesSecondaryToolsToMenu() {
        var exited = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) { PuppetTopBar({}, {}, onExit = { exited = true }, onSave = {}) }
            }
        }
        compose.onNodeWithContentDescription("Save actor revision").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back from Actor Studio").performClick()
        compose.runOnIdle { assertTrue(exited) }
        compose.onNodeWithContentDescription("More actor tools").performClick()
        compose.onNodeWithContentDescription("Fit puppet canvas").assertIsDisplayed()
    }
}
