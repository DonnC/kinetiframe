package com.donnclab.kinetiframe.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.ui.canvas.SelectionTransformOverlay
import com.donnclab.kinetiframe.studio.ui.canvas.SelectionTransformState
import com.donnclab.kinetiframe.studio.ui.dock.SelectionActionMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SelectionMoveControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun moveContextActionDispatchesExplicitModeAndDismissesMenu() {
        var dispatched: StudioIntent? = null
        var dismissed = false
        compose.setContent {
            KinetiFrameTheme {
                SelectionActionMenu(true, { dismissed = true }, StudioState(), { dispatched = it })
            }
        }
        compose.onNodeWithContentDescription("Move selection").performClick()
        compose.runOnIdle {
            assertEquals(StudioIntent.Selection.SetInteractionMode(SelectionInteractionMode.MOVE), dispatched)
            assertEquals(true, dismissed)
        }
    }

    @Test fun draggingTinySelectionCenterInMoveModeTranslatesInsteadOfMovingPivot() {
        lateinit var transform: SelectionTransformState
        compose.setContent {
            transform = remember { SelectionTransformState() }
            Box(Modifier.size(300.dp, 200.dp).testTag("selection")) {
                SelectionTransformOverlay(
                    Rect(98f, 98f, 102f, 102f), transform,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), null, 1f, 1f,
                    onTapInside = {}, onTapOutside = {}, interactionMode = SelectionInteractionMode.MOVE,
                )
            }
        }
        compose.onNodeWithTag("selection").performTouchInput {
            down(Offset(100f, 100f))
            moveTo(Offset(130f, 120f))
            up()
        }
        compose.runOnIdle {
            assertEquals(30f, transform.matrix.values[12], .001f)
            assertEquals(20f, transform.matrix.values[13], .001f)
            assertEquals(Offset.Zero, transform.pivotOffset)
        }
    }

    @Test fun directPivotDragPreservesContactOffsetWithoutMovingArtwork() {
        lateinit var transform: SelectionTransformState
        compose.setContent {
            transform = remember { SelectionTransformState() }
            Box(Modifier.size(300.dp, 200.dp).testTag("selection")) {
                SelectionTransformOverlay(
                    Rect(0f, 0f, 200f, 200f),
                    transform,
                    floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
                    null,
                    1f,
                    1f,
                    onTapInside = {},
                    onTapOutside = {},
                )
            }
        }
        compose.onNodeWithTag("selection").performTouchInput {
            down(Offset(103f, 100f))
            moveTo(Offset(133f, 120f))
            up()
        }
        compose.runOnIdle {
            assertEquals(Offset(30f, 20f), transform.pivotOffset)
            assertEquals(0f, transform.matrix.values[12], .001f)
            assertEquals(0f, transform.matrix.values[13], .001f)
        }
    }
}
