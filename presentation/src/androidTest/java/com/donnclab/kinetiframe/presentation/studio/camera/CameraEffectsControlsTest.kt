package com.donnclab.kinetiframe.presentation.studio.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.width
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.studio.timeline.AudioFadeDialog
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraEffectsControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun timingPhasesAreAvailableThroughTheContextControl() {
        compose.setContent {
            KinetiFrameTheme {
                com.donnclab.kinetiframe.presentation.studio.camera.CameraShotEditor(
                    CameraShot("pan", CameraAction.PAN, 0, 24),
                    StudioState(animation = AnimationState(totalFrames = 48)),
                    {},
                    {},
                    {},
                    content = { _, _, _ -> },
                )
            }
        }
        compose.onNodeWithContentDescription("Movement timing and customization").performClick()
        compose.onNodeWithText("Wait · 0.00 s").assertExists()
        compose.onNodeWithText("Settle · 0.00 s").assertExists()
        compose.onNodeWithText("All phases stay inside the selected frame range.").assertExists()
        compose.onNodeWithContentDescription("Wait duration")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(6f) }
        compose.onNodeWithText("Wait · 0.50 s").assertExists()
    }

    @Test fun occupiedRangesAreVisibleAndConflictingSaveIsDisabledBeforeClick() {
        val occupied = CameraShot("impact", CameraAction.SHAKE, 8, 21)
        val draft = CameraShot("pan", CameraAction.PAN, 12, 24)
        compose.setContent {
            KinetiFrameTheme {
                com.donnclab.kinetiframe.presentation.studio.camera.CameraShotEditor(
                    draft,
                    StudioState(
                        project = com.donnclab.kinetiframe.core.document.ProjectDocument(
                            cameraShots = listOf(occupied),
                        ),
                        animation = AnimationState(totalFrames = 96),
                    ),
                    {},
                    {},
                    {},
                    content = { _, _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Occupied · Impact shake 9–21").assertExists()
        compose.onNodeWithContentDescription("Save camera effect").assertIsNotEnabled()
        compose.onNodeWithText("Range overlaps 9–21. Choose free frames.").assertExists()
    }

    @Test fun previewPublishesWorkspaceLayoutButFittedDialogDoesNot() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            KinetiFrameTheme {
                com.donnclab.kinetiframe.presentation.studio.canvas.CanvasArea(
                    cameraPreview = true,
                    state = StudioState(
                        project = com.donnclab.kinetiframe.core.document.ProjectDocument(
                            cameraShots =
                            listOf(CameraShot("impact", CameraAction.SHAKE, 8, 21)),
                        ),
                    ),
                    onIntent = intents::add,
                )
            }
        }
        compose.runOnIdle {
            assertTrue(
                intents.filterIsInstance<StudioIntent.View.SetViewportSize>().any {
                    it.width > 0 &&
                        it.height > 0
                },
            )
        }
    }

    @Test fun fittedCameraPreviewDoesNotChangeWorkspaceLayout() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            KinetiFrameTheme {
                com.donnclab.kinetiframe.presentation.studio.canvas.CanvasArea(
                    cameraPreview = true,
                    fitStage = true,
                    cameraPoseOverride = CameraPose(),
                    state = StudioState(),
                    onIntent = intents::add,
                )
            }
        }
        compose.runOnIdle { assertTrue(intents.filterIsInstance<StudioIntent.View.SetViewportSize>().isEmpty()) }
    }

    @Test fun choosingAnActionAndConfirmingEmitsADurableShot() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            KinetiFrameTheme {
                CameraWorkspaceControls(
                    StudioState(animation = AnimationState(totalFrames = 48)),
                    false,
                    {},
                    intents::add,
                    content = { _, _, _ -> Box(androidx.compose.ui.Modifier) },
                )
            }
        }
        compose.onNodeWithContentDescription("Camera effects").performClick()
        compose.onNodeWithText("Push in").performClick()
        compose.onNodeWithContentDescription("Save camera effect").performClick()
        compose.runOnIdle {
            val shot = intents.filterIsInstance<StudioIntent.Camera.Put>().single().shot
            assertEquals(CameraAction.PUSH_IN, shot.action)
            assertTrue(shot.end.zoom > shot.start.zoom)
        }
    }

    @Test fun cancellingAnAudioFadeDoesNotCommit() {
        var committed = false
        compose.setContent {
            KinetiFrameTheme {
                AudioFadeDialog(
                    AudioClipPlacement("a", "asset", 0, "Sound", 0, 48),
                    24,
                    {},
                    { _, _ -> committed = true },
                )
            }
        }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(committed) }
    }
}
