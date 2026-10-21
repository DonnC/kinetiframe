package com.donnclab.kinetiframe.ui.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.camera.CameraAction
import com.donnclab.kinetiframe.engine.camera.CameraShot
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.canvas.CanvasArea
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraWorkspaceInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val intents = mutableListOf<TimelineIntent>()

    private fun show(preview: Boolean = false, fitted: Boolean = false) {
        rule.setContent {
            KinetiFrameTheme {
                CanvasArea(
                    state = TimelineState(
                        project = ProjectDocument(
                            stage = StageDimensions(4096, 4096),
                            cameraShots = listOf(CameraShot("test-shot", CameraAction.PAN, 0, 12)),
                        ),
                    ),
                    onIntent = { intents += it },
                    cameraPreview = preview,
                    fitStage = fitted,
                    modifier = Modifier.fillMaxSize().testTag("workspace"),
                )
            }
        }
    }

    private fun twoFingerTransform() {
        rule.onNodeWithTag("workspace").performTouchInput {
            val first = center - Offset(40f, 0f)
            val second = center + Offset(40f, 0f)
            down(0, first)
            down(1, second)
            moveTo(0, first + Offset(10f, 12f))
            moveTo(1, second + Offset(40f, 12f))
            up(0)
            up(1)
        }
        rule.waitForIdle()
    }

    @Test fun cameraProjectAllowsDrawingAndNavigationWhenOpened() {
        show()
        rule.onNodeWithTag("workspace").performTouchInput {
            swipe(center - Offset(30f, 0f), center + Offset(30f, 0f))
        }
        rule.waitForIdle()
        assertTrue(intents.any { it is TimelineIntent.Draw.BakeStroke })
        intents.clear()
        twoFingerTransform()
        assertTrue(intents.any { it is TimelineIntent.View.UpdateTransform })
        assertFalse(intents.any { it is TimelineIntent.Draw.BakeStroke })
    }

    @Test fun explicitCameraPreviewNavigatesWithoutPainting() {
        show(preview = true)
        twoFingerTransform()
        assertTrue(intents.any { it is TimelineIntent.View.UpdateTransform })
        assertFalse(intents.any { it is TimelineIntent.Draw.BakeStroke })
    }

    @Test fun fittedCameraDialogNeverChangesMainWorkspace() {
        show(preview = true, fitted = true)
        twoFingerTransform()
        assertFalse(intents.any { it is TimelineIntent.View.UpdateTransform })
        assertFalse(intents.any { it is TimelineIntent.Draw.BakeStroke })
    }
}
