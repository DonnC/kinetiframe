package com.donnclab.kinetiframe.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.top
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.presentation.library.packs.PackSaveTagsDialog
import com.donnclab.kinetiframe.presentation.studio.EditorTopBar
import com.donnclab.kinetiframe.presentation.studio.timeline.FrameCard
import com.donnclab.kinetiframe.presentation.studio.timeline.StudioFocusControls
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineAssetSourceMenu
import com.donnclab.kinetiframe.presentation.studio.timeline.TimelineControls
import com.donnclab.kinetiframe.presentation.studio.timeline.TracksWorkspaceDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CanvasFirstStudioTest {
    @get:Rule val compose = createComposeRule()

    @Composable
    private fun TestAppBar(onTracksOpen: () -> Unit = {}) {
        var overflow by remember { mutableStateOf(false) }
        EditorTopBar(
            state = StudioState(), onIntent = {}, onHomeClick = {},
            onStudioClick = {}, onGridSettingsClick = {}, onOnionSettingsClick = {},
            showOverflowMenu = overflow, onOverflowMenuToggle = { overflow = it },
            onTracksOpen = onTracksOpen,
        )
    }

    @Test fun timelineUsesOneRowEvenDuringPlayback() {
        compose.setContent {
            KinetiFrameTheme {
                TimelineControls(
                    state = StudioState(animation = AnimationState(isPlaying = true)),
                    onIntent = {},
                    modifier = Modifier.width(700.dp).testTag("dock"),
                )
            }
        }
        compose.onNodeWithTag("dock").assertHeightIsEqualTo(56.dp)
        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText("Playback FPS").assertDoesNotExist()
    }

    @Test fun tracksAreAnExplicitActionNotAPermanentCollapsedRow() {
        var opened = false
        compose.setContent {
            KinetiFrameTheme {
                TestAppBar { opened = true }
            }
        }
        compose.onNodeWithText("Tracks · 0 clips").assertDoesNotExist()
        compose.onNodeWithContentDescription("Edit asset tracks").performClick()
        assertTrue(opened)
        compose.onNodeWithContentDescription("More Settings").performClick()
        compose.onAllNodesWithContentDescription("Edit asset tracks").assertCountEquals(1)
    }

    @Test fun speedIsOnlyExpandedOnRequest() {
        compose.setContent { KinetiFrameTheme { TestAppBar() } }
        compose.onNodeWithText("Playback FPS").assertDoesNotExist()
        compose.onNodeWithContentDescription("More Settings").performClick()
        compose.onNodeWithContentDescription("Playback speed").performClick()
        compose.onNodeWithText("Playback FPS").assertExists()
    }

    @Test fun focusRetainsTransportAndRecovery() {
        var restored = false
        var intent: StudioIntent? = null
        compose.setContent {
            KinetiFrameTheme { StudioFocusControls(false, { intent = it }, { restored = true }) }
        }
        compose.onNodeWithContentDescription("Play").performClick()
        assertEquals(StudioIntent.Playback.Play, intent)
        compose.onNodeWithContentDescription("Restore studio controls").performClick()
        assertTrue(restored)
    }

    @Test fun workspaceDoesNotRemeasureUnderlyingCanvas() {
        var open by mutableStateOf(false)
        compose.setContent {
            KinetiFrameTheme {
                Box(Modifier.fillMaxSize().testTag("canvas"))
                if (open) {
                    TracksWorkspaceDialog(false, {}, { open = false }) {
                        Text("Clip lanes")
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("canvas").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { open = true }
        compose.onNodeWithText("Clip lanes").assertExists()
        assertEquals(before, compose.onNodeWithTag("canvas").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Close asset tracks").performClick()
        compose.onNodeWithText("Clip lanes").assertDoesNotExist()
    }

    @Test fun narrowDockKeepsInlineFrameCreationReachable() {
        var intent: StudioIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                TimelineControls(
                    state = StudioState(),
                    onIntent = { intent = it },
                    modifier = Modifier.width(360.dp).testTag("dock"),
                )
            }
        }
        compose.onNodeWithTag("dock").assertHeightIsEqualTo(56.dp)
        compose.onNodeWithContentDescription("Add Frame").performClick()
        assertEquals(StudioIntent.Frame.AddFrame, intent)
        compose.onNodeWithContentDescription("Layers").assertExists()
        compose.onNodeWithContentDescription("Focus canvas").assertDoesNotExist()
    }

    @Test fun audioSourcesAppearOnlyUnderAudio() {
        var imported = false
        compose.setContent {
            KinetiFrameTheme {
                TimelineAssetSourceMenu(true, {}, {}, { imported = true }, {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Record audio").assertDoesNotExist()
        compose.onNodeWithContentDescription("Audio").performClick()
        compose.onNodeWithContentDescription("Record audio").assertExists()
        compose.onNodeWithContentDescription("Import audio from device").performClick()
        assertTrue(imported)
    }

    @Test fun packSaveEditsMetadataWithoutMutatingTheDraftBeforeConfirmation() {
        var saved: Map<Int, PackFrameTag>? = null
        val original = mapOf(0 to PackFrameTag("Torso", "Reusable"))
        compose.setContent {
            KinetiFrameTheme {
                PackSaveTagsDialog(1, original, 0, {}, { saved = it })
            }
        }
        compose.onNodeWithText("Torso").performTextReplacement("New torso")
        assertEquals("Torso", original.getValue(0).name)
        compose.onNodeWithContentDescription("Save pack").performClick()
        assertEquals(PackFrameTag("New torso", "Reusable"), saved?.get(0))
    }

    @Test fun cancellingPackMetadataDoesNotSave() {
        var cancelled = false
        var saved = false
        compose.setContent {
            KinetiFrameTheme {
                PackSaveTagsDialog(1, emptyMap(), 0, { cancelled = true }, { saved = true })
            }
        }
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertTrue(cancelled)
        assertEquals(false, saved)
    }

    @Test fun frameNumberStaysInsideTheThumbnail() {
        compose.setContent {
            KinetiFrameTheme {
                Box(Modifier.testTag("frame")) { FrameCard(2, true, onClick = {}) }
            }
        }
        compose.onNodeWithTag("frame").assertHeightIsEqualTo(40.dp)
        val frame = compose.onNodeWithTag("frame").fetchSemanticsNode().boundsInRoot
        val number = compose.onNodeWithText("3").fetchSemanticsNode().boundsInRoot
        assertTrue(number.bottom <= frame.bottom)
        assertTrue(number.top >= frame.top)
    }
}
