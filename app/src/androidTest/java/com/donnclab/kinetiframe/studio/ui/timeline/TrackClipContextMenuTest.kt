package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.AudioTrackSettings
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TrackClipContextMenuTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val temporary = TemporaryFolder()
    private val clip = AudioClipPlacement("voice", "asset", 0, "Voice", 0, 8)

    private fun show(locked: Boolean = false, playhead: Int = 3, onIntent: (StudioIntent) -> Unit = {}) {
        val repository = PerformanceRepository(DocumentFileStore(temporary.newFolder()))
        val state = StudioState(
            project = ProjectDocument(
                audioClips = mapOf(clip.clipId to clip),
                audioTracks = mapOf(0 to AudioTrackSettings(isLocked = locked)),
            ),
            animation = AnimationState(currentFrame = playhead, totalFrames = 8),
        )
        compose.setContent {
            MaterialTheme {
                TimelineTracksPanel(
                    state = state,
                    onIntent = onIntent,
                    performanceRepository = repository,
                    onEditPlacement = {},
                    onAddToLibrary = {},
                    workspaceMode = true,
                )
            }
        }
        compose.onNodeWithText("Voice").performClick()
    }

    @Test fun firstTapShowsIconPaletteAndDuplicateDispatchesOnce() {
        val intents = mutableListOf<StudioIntent>()
        show(onIntent = { intents.add(it) })
        compose.onNodeWithText("Duplicate").assertDoesNotExist()
        compose.onNodeWithContentDescription("Duplicate").performClick()
        compose.runOnIdle { assertEquals(listOf(StudioIntent.Audio.Duplicate("voice")), intents) }
        compose.onNodeWithContentDescription("Duplicate").assertDoesNotExist()
    }

    @Test fun lockedLaneDisablesEditsButStillAllowsSavingToLibrary() {
        show(locked = true)
        listOf("Duplicate", "Split at playhead", "Delete", "Volume", "Position clip precisely").forEach {
            compose.onNodeWithContentDescription(it).assertIsNotEnabled()
        }
        compose.onNodeWithContentDescription("Add to library").assertIsEnabled()
    }

    @Test fun splitIsDisabledAtBoundary() {
        show(playhead = 0)
        compose.onNodeWithContentDescription("Split at playhead").assertIsNotEnabled()
    }
}
