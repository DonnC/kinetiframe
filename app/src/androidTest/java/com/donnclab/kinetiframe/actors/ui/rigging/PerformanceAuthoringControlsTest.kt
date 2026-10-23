package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.actors.ui.rigging.PerformanceDurationMenu
import com.donnclab.kinetiframe.actors.ui.rigging.PerformanceFrameRuler
import com.donnclab.kinetiframe.actors.ui.rigging.PuppetTopBar
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ui.EditorTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class PerformanceAuthoringControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun drawingToolbarOpensSkeletonEditing() {
        val intents = mutableListOf<StudioIntent>()
        compose.setContent {
            KinetiFrameTheme {
                EditorTopBar(
                    state = StudioState(), onIntent = { intents += it }, onHomeClick = {}, onStudioClick = {},
                    onGridSettingsClick = {}, onOnionSettingsClick = {}, isActorStudio = true,
                    showOverflowMenu = false, onOverflowMenuToggle = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Edit skeleton joints").performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(
                    StudioIntent.Rigging.SetStudioMode(com.donnclab.kinetiframe.studio.StudioMode.RIG_SETUP),
                    StudioIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.studio.ActorEditTarget.JOINT_END),
                ),
                intents,
            )
        }
    }

    @Test fun actorCanBeSavedWhileDrawing() {
        var saved = false
        compose.setContent {
            KinetiFrameTheme {
                EditorTopBar(
                    state = StudioState(), onIntent = {}, onHomeClick = {}, onStudioClick = {},
                    onGridSettingsClick = {}, onOnionSettingsClick = {}, isActorStudio = true,
                    showOverflowMenu = false, onOverflowMenuToggle = {}, onSaveActorClick = { saved = true },
                )
            }
        }
        compose.onNodeWithContentDescription("Save actor revision").performClick()
        compose.runOnIdle { assertEquals(true, saved) }
    }

    @Test fun performanceToolbarExposesActorUndo() {
        var intent: StudioIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                PuppetTopBar({ intent = it }, {}, performanceOnly = true, canUndo = true)
            }
        }
        compose.onNodeWithContentDescription("Undo actor edit").performClick()
        compose.runOnIdle { assertEquals(StudioIntent.Actor.Undo, intent) }
    }

    @Test fun diamondSelectsExactSavedTime() {
        var selected = -1L
        compose.setContent {
            KinetiFrameTheme {
                PerformanceFrameRuler(12, 1_000_000, 0, listOf(ActorPoseKey(500_000, emptyMap())), { selected = it })
            }
        }
        compose.onNodeWithContentDescription("Frame 7, pose key").performClick()
        compose.runOnIdle { assertEquals(500_000L, selected) }
    }

    @Test fun durationCanBeChosenWithoutRepeatedFrameTaps() {
        var selected = -1L
        compose.setContent {
            KinetiFrameTheme {
                PerformanceDurationMenu(2_000_000, 83_333, 30_000_000, 83_333) { selected = it }
            }
        }
        compose.onNodeWithText(String.format(Locale.getDefault(), "%.2f s", 2.0)).performClick()
        compose.onNodeWithText(String.format(Locale.getDefault(), "%.1f seconds", 5.0)).performClick()
        compose.runOnIdle { assertEquals(5_000_000L, selected) }
    }
}
