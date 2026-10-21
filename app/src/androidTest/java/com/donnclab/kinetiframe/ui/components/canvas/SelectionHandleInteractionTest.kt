package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectionHandleInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val bounds = Rect(100f, 100f, 300f, 300f)

    @Test fun cornerDragHasExactReleaseAndOppositeAnchor() {
        val transform = SelectionTransformState()
        var completions = 0
        compose.setContent {
            Box(Modifier.fillMaxSize().testTag("handles")) {
                SelectionTransformOverlay(
                    bounds, transform, floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
                    null, 1f, 1f, {}, {}, onDragEnd = { completions++ },
                )
            }
        }
        compose.onNodeWithTag("handles").performTouchInput {
            down(Offset(100f, 100f))
            moveTo(Offset(80f, 60f))
            up()
        }
        compose.runOnIdle {
            assertEquals(80f, transform.matrix.map(bounds.topLeft).x, .01f)
            assertEquals(60f, transform.matrix.map(bounds.topLeft).y, .01f)
            assertEquals(300f, transform.matrix.map(bounds.bottomRight).x, .01f)
            assertEquals(300f, transform.matrix.map(bounds.bottomRight).y, .01f)
            assertEquals(1, completions)
        }
    }

    @Test fun cancelledHandleGestureRestoresStart() {
        val transform = SelectionTransformState()
        compose.setContent {
            Box(Modifier.fillMaxSize().testTag("handles")) {
                SelectionTransformOverlay(
                    bounds,
                    transform,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
                    null,
                    1f,
                    1f,
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithTag("handles").performTouchInput {
            down(Offset(100f, 100f))
            moveTo(Offset(80f, 60f))
            cancel()
        }
        compose.runOnIdle {
            assertEquals(bounds.topLeft.x, transform.matrix.map(bounds.topLeft).x, .01f)
            assertEquals(bounds.topLeft.y, transform.matrix.map(bounds.topLeft).y, .01f)
        }
    }
}
