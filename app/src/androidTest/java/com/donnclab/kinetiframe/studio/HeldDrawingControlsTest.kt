package com.donnclab.kinetiframe.studio

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.donnclab.kinetiframe.studio.ui.timeline.HoldDrawingDialog
import com.donnclab.kinetiframe.studio.ui.timeline.LinkedDrawingScopeDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HeldDrawingControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun holdRangeRejectsSingleFrameAndCommitsZeroBasedEnd() {
        var end: Int? = null
        compose.setContent { MaterialTheme { HoldDrawingDialog(0, 24, {}, { end = it }) } }
        compose.onNodeWithText("24").performTextReplacement("1")
        compose.onNodeWithContentDescription("Hold range").assertIsNotEnabled()
        compose.onNodeWithText("1").performTextReplacement("3")
        compose.onNodeWithContentDescription("Hold range").performClick()
        compose.runOnIdle { assertEquals(2, end) }
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
