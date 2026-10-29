package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraWorkspaceInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val intents = mutableListOf<StudioIntent>()

    private fun show(preview: Boolean = false, fitted: Boolean = false) {
        rule.setContent {
            KinetiFrameTheme {
                CanvasArea(
                    state = StudioState(
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
        assertTrue(intents.any { it is StudioIntent.Draw.BakeStroke })
        intents.clear()
        twoFingerTransform()
        assertTrue(intents.any { it is StudioIntent.View.UpdateTransform })
        assertFalse(intents.any { it is StudioIntent.Draw.BakeStroke })
    }

    @Test fun explicitCameraPreviewNavigatesWithoutPainting() {
        show(preview = true)
        twoFingerTransform()
        assertTrue(intents.any { it is StudioIntent.View.UpdateTransform })
        assertFalse(intents.any { it is StudioIntent.Draw.BakeStroke })
    }

    @Test fun fittedCameraDialogNeverChangesMainWorkspace() {
        show(preview = true, fitted = true)
        twoFingerTransform()
        assertFalse(intents.any { it is StudioIntent.View.UpdateTransform })
        assertFalse(intents.any { it is StudioIntent.Draw.BakeStroke })
    }
}
