package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.ui.actors.sketch.ActorDrawingModeButton
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActorDrawingModeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wholeDrawingEntryAndReversibleCutoutMode() {
        var opened = false
        var enabled = true
        compose.setContent { MaterialTheme { ActorDrawingModeButton(true, true, { opened = true }, { enabled = it }) } }
        compose.onNodeWithContentDescription("Actor drawing workflow").performClick()
        compose.onNodeWithText("Edit continuous drawing").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithContentDescription("Actor drawing workflow").performClick()
        compose.onNodeWithText("Use cutout artwork").performClick()
        compose.runOnIdle { assertFalse(enabled) }
    }
}
