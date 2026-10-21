package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.rigging.PuppetTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PuppetWorkspaceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun studioBarReturnsToRasterAndTogglesHierarchy() {
        var intent: TimelineIntent? = null
        var toggles = 0
        compose.setContent { PuppetTopBar({ intent = it }, { toggles++ }) }
        compose.onNodeWithContentDescription("Show or hide character hierarchy").performClick()
        compose.onNodeWithContentDescription("Return to frame-by-frame workspace").performClick()
        compose.runOnIdle {
            assertEquals(1, toggles)
            assertEquals(TimelineIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING), intent)
        }
    }
}
