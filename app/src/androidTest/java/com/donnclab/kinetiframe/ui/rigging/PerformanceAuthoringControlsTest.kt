package com.donnclab.kinetiframe.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.components.EditorTopBar
import com.donnclab.kinetiframe.ui.components.rigging.PerformanceDurationMenu
import com.donnclab.kinetiframe.ui.components.rigging.PerformanceFrameRuler
import com.donnclab.kinetiframe.ui.components.rigging.PuppetTopBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class PerformanceAuthoringControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun drawingToolbarOpensSkeletonEditing() {
        val intents = mutableListOf<TimelineIntent>()
        compose.setContent {
            KinetiFrameTheme {
                EditorTopBar(
                    state = TimelineState(), onIntent = { intents += it }, onHomeClick = {}, onStudioClick = {},
                    onGridSettingsClick = {}, onOnionSettingsClick = {}, isActorStudio = true,
                    showOverflowMenu = false, onOverflowMenuToggle = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Edit skeleton joints").performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(
                    TimelineIntent.Rigging.SetStudioMode(com.donnclab.kinetiframe.ui.StudioMode.RIG_SETUP),
                    TimelineIntent.Actor.SetEditTarget(com.donnclab.kinetiframe.ui.ActorEditTarget.JOINT_END),
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
                    state = TimelineState(), onIntent = {}, onHomeClick = {}, onStudioClick = {},
                    onGridSettingsClick = {}, onOnionSettingsClick = {}, isActorStudio = true,
                    showOverflowMenu = false, onOverflowMenuToggle = {}, onSaveActorClick = { saved = true },
                )
            }
        }
        compose.onNodeWithContentDescription("Save actor revision").performClick()
        compose.runOnIdle { assertEquals(true, saved) }
    }

    @Test fun performanceToolbarExposesActorUndo() {
        var intent: TimelineIntent? = null
        compose.setContent {
            KinetiFrameTheme {
                PuppetTopBar({ intent = it }, {}, performanceOnly = true, canUndo = true)
            }
        }
        compose.onNodeWithContentDescription("Undo actor edit").performClick()
        compose.runOnIdle { assertEquals(TimelineIntent.Actor.Undo, intent) }
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
