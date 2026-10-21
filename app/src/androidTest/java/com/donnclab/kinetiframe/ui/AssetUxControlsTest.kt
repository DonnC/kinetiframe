package com.donnclab.kinetiframe.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.audio.AudioPreviewControls
import com.donnclab.kinetiframe.ui.audio.AudioPreviewIntent
import com.donnclab.kinetiframe.ui.audio.AudioPreviewState
import com.donnclab.kinetiframe.ui.components.dock.SelectionActionMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssetUxControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun audioPlayIsAccessibleAndDispatchesTransport() {
        var intent: AudioPreviewIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                AudioPreviewControls(AudioPreviewState(ready = true, durationMs = 2000), {
                    intent =
                        it
                })
            }
        }
        compose.onNodeWithContentDescription("Play audio").assertIsEnabled().performClick()
        assertEquals(AudioPreviewIntent.Toggle, intent)
    }

    @Test fun failedAudioDoesNotOfferEnabledPlayback() {
        compose.setContent { KinetiFrameTheme { AudioPreviewControls(AudioPreviewState(error = "Missing file"), {}) } }
        compose.onNodeWithContentDescription("Play audio").assertIsNotEnabled()
        compose.onNodeWithText("Missing file").assertExists()
    }

    @Test fun compactSelectionActionsPreserveCopyIntent() {
        var intent: TimelineIntent? = null
        compose.setContent { KinetiFrameTheme { SelectionActionMenu(true, {}, TimelineState(), { intent = it }) } }
        compose.onNodeWithContentDescription("Copy").performClick()
        assertEquals(TimelineIntent.Selection.Copy, intent)
    }
}
