package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.presentation.studio.timeline.LinkedDrawingScopeDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HeldDrawingControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun holdRangeNeedsMoreThanOneFrameAndShowsItsSpan() {
        var confirmed = false
        var pick by mutableStateOf(HoldRangePick(source = 3, other = 3))
        compose.setContent { MaterialTheme { HoldRangeBar(pick, { confirmed = true }, {}) } }
        compose.onNodeWithContentDescription("Hold range").assertIsNotEnabled()
        compose.runOnIdle { pick = pick.copy(other = 0) }
        compose.onNodeWithText("Hold frames 1–4").assertExists()
        compose.onNodeWithContentDescription("Hold range").performClick()
        compose.runOnIdle { assertEquals(true, confirmed) }
    }

    @Test fun editingScopeRequiresAnExplicitChoice() {
        var scope: String? = null
        compose.setContent {
            MaterialTheme { LinkedDrawingScopeDialog(3, {}, { scope = "local" }, { scope = "shared" }, {}) }
        }
        compose.runOnIdle { assertNull(scope) }
        compose.onNodeWithText("All linked frames").performClick()
        compose.runOnIdle { assertEquals("shared", scope) }
        compose.onNodeWithText("This frame only").performClick()
        compose.runOnIdle { assertEquals("local", scope) }
    }
}
