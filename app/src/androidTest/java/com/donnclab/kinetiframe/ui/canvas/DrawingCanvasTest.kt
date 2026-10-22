package com.donnclab.kinetiframe.ui.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.components.canvas.BufferedDrawingCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawingCanvasTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test fun stationaryTapCommitsOneContact() {
        var points: List<TouchPoint>? = null
        composeTestRule.setContent {
            BufferedDrawingCanvas(
                StudioState(),
                null,
                { samples, _ -> points = samples },
                Modifier.testTag("drawing_canvas"),
            )
        }
        composeTestRule.onNodeWithTag("drawing_canvas").performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(1, points?.size)
    }

    @Test fun cancelledGestureDoesNotCommit() {
        var commits = 0
        composeTestRule.setContent {
            BufferedDrawingCanvas(
                StudioState(),
                null,
                { _, _ -> commits++ },
                Modifier.testTag("drawing_canvas"),
            )
        }
        composeTestRule.onNodeWithTag("drawing_canvas").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(20f, 0f))
            cancel()
        }
        composeTestRule.waitForIdle()
        assertEquals(0, commits)
    }

    @Test
    fun testDrawingStrokeEmitsPoints() {
        var strokePoints: List<TouchPoint>? = null

        composeTestRule.setContent {
            BufferedDrawingCanvas(
                studioState = StudioState(),
                currentFrameBitmap = null,
                onStrokeComplete = { points, _ ->
                    strokePoints = points
                },
                modifier = Modifier.testTag("drawing_canvas"),
            )
        }

        // Perform a swipe gesture
        composeTestRule.onNodeWithTag("drawing_canvas").performTouchInput {
            swipe(
                start = centerLeft,
                end = centerRight,
                durationMillis = 100,
            )
        }

        // Wait for the stroke to complete (it completes on ACTION_UP)
        composeTestRule.waitForIdle()

        // Verify points were emitted
        assertTrue("Stroke should emit points", strokePoints?.isNotEmpty() == true)
    }
}
