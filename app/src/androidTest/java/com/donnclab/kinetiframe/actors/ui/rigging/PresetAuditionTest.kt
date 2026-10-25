package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.rig.presets.RotationChannel
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ui.actorstudio.rigging.ActorMotionTimingMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PresetAuditionTest {
    @get:Rule val compose = createComposeRule()
    private val recipe = ActorMotionPreset(
        id = "wave",
        name = "Wave",
        description = "",
        durationSeconds = 2f,
        requiredBones = listOf("head"),
        channels = listOf(RotationChannel("head")),
    )

    @Test fun darkSheetCardCanSelectAndApply() {
        var selected: String? = null
        var applied = false
        compose.setContent {
            PresetLibrarySection(listOf(recipe), null, true, { selected = it }, { applied = true }, true)
        }
        compose.onNodeWithText("Wave").performClick()
        compose.onNodeWithContentDescription("Apply retargeting").performClick()
        compose.runOnIdle {
            assertEquals("wave", selected)
            assertEquals(true, applied)
        }
    }

    @Test fun auditionHasPlaybackAndExplicitReplacementNotice() {
        var played = false
        compose.setContent {
            PresetAuditionControls(AppliedActorPreset(recipe), false, true, { played = true }, {}, {}, null)
        }
        compose.onNodeWithContentDescription("Preview motion").performClick()
        compose.onNodeWithText("Apply replaces this performance's keys · undo available").assertExists()
        compose.runOnIdle { assertEquals(true, played) }
    }

    @Test fun timingMenuEmitsSavedAuthoringIntent() {
        var selected: StudioIntent? = null
        compose.setContent {
            ActorMotionTimingMenu(StudioState(), { selected = it }, true)
        }
        compose.onNodeWithContentDescription("Motion timing").performClick()
        compose.onNodeWithText("Ease · pose to pose").performClick()
        compose.runOnIdle {
            assertEquals(
                StudioIntent.ActorMotion.SetInterpolation(
                    CurveInterpolation.EASE_IN_OUT,
                ),
                selected,
            )
        }
    }
}
