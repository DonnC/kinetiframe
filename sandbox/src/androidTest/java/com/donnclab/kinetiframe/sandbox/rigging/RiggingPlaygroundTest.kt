// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.sandbox.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.center
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import com.donnclab.kinetiframe.sandbox.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RiggingPlaygroundTest {
    @get:Rule val rule = createComposeRule()

    @Test fun meshPreviewCanBeSelectedWithoutReplacingRiggingWorkflow() {
        val controller = SandboxController()
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        rule.setContent {
            val state by controller.state.collectAsState()
            MaterialTheme { RiggingScreen(state, controller::handleIntent) }
        }
        rule.onNodeWithContentDescription(resources.getString(R.string.preview_mesh)).performClick()
        rule.runOnIdle { assertEquals(RigPreviewMode.SKIN_MESH, controller.state.value.previewMode) }
        rule.onNodeWithContentDescription(resources.getString(R.string.preview_bones)).assertExists()
        controller.close()
    }

    @Test fun draggingTargetCapturesAndResetRestoresFixture() {
        val controller = SandboxController()
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        rule.setContent {
            val state by controller.state.collectAsState()
            MaterialTheme { RiggingScreen(state, controller::handleIntent) }
        }
        rule.onNodeWithText(resources.getString(R.string.record)).performClick()
        rule.onNodeWithContentDescription(resources.getString(R.string.rigging_canvas)).performTouchInput {
            swipe(center, centerRight)
        }
        rule.runOnIdle { assertTrue(controller.state.value.capture.targets.isNotEmpty()) }
        rule.onNodeWithText(resources.getString(R.string.reset)).performClick()
        rule.onNodeWithText(resources.getString(R.string.sample_count, 0, 1)).assertIsDisplayed()
        assertEquals(SandboxState(), controller.state.value)
        controller.close()
    }
}
