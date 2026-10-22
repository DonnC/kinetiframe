package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.ui.ActorStudioState
import com.donnclab.kinetiframe.ui.PerformanceTakePhase
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.components.rigging.PerformanceTakeControls
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PerformanceTakeControlsTest {
    @Test fun recordingShowsTakeElapsedTimeAndDirectCharacterMoveToggle() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            MaterialTheme {
                PerformanceTakeControls(
                    StudioState(
                        actor = ActorStudioState(
                            takePhase = PerformanceTakePhase.RECORDING,
                            takeStartMicros = 40_000_000,
                            previewTimeMicros = 41_000_000,
                        ),
                    ),
                    intents::add,
                )
            }
        }
        compose.onNodeWithText("Recording · 1.0s / 30s").assertExists()
        compose.onNodeWithContentDescription("Move whole character").performClick()
        assertEquals(listOf(StudioIntent.ActorMotion.SetTakeMoveCharacter(true)), intents)
    }

    @get:Rule val compose = createComposeRule()

    @Test fun recordIsDirectAndReviewExposesKeepRetakeAndDiscard() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            MaterialTheme {
                PerformanceTakeControls(
                    StudioState(
                        actor =
                        ActorStudioState(takePhase = PerformanceTakePhase.REVIEW),
                    ),
                    intents::add,
                )
            }
        }
        compose.onNodeWithContentDescription("Replay take").assertExists()
        compose.onNodeWithContentDescription("Keep take").performClick()
        compose.onNodeWithContentDescription("Retake movement").performClick()
        compose.onNodeWithContentDescription("Discard take").performClick()
        assertEquals(
            listOf(
                StudioIntent.ActorMotion.KeepTake,
                StudioIntent.ActorMotion.Retake,
                StudioIntent.ActorMotion.CancelTake,
            ),
            intents,
        )
    }

    @Test fun idleStartsRecordingWithoutOpeningAConfigurationDialog() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent { MaterialTheme { PerformanceTakeControls(StudioState(), intents::add) } }
        compose.onNodeWithContentDescription("Record movement").performClick()
        assertEquals(listOf(StudioIntent.ActorMotion.StartTake), intents)
    }
}
