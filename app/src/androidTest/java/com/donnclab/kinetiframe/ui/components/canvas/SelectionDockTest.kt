package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.components.dock.SelectionDock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectionDockTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelIsNotApply() {
        var last: StudioIntent? = null
        compose.setContent { KinetiFrameTheme { SelectionDock(state = StudioState(), onIntent = { last = it }) } }
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.runOnIdle { assertEquals(StudioIntent.Selection.Cancel, last) }
    }

    @Test fun skewIsDiscoverableWithoutPersistentControls() {
        var last: StudioIntent? = null
        compose.setContent { KinetiFrameTheme { SelectionDock(state = StudioState(), onIntent = { last = it }) } }
        compose.onNodeWithContentDescription("Selection actions").performClick()
        compose.onNodeWithText("Skew side handles").performClick()
        compose.runOnIdle { assertEquals(StudioIntent.Selection.SetSkewMode(true), last) }
    }
}
