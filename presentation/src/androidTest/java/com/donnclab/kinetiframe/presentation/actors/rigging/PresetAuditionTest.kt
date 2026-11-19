package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.donnclab.kinetiframe.actors.rig.presets.ActorPreset
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import com.donnclab.kinetiframe.presentation.actors.rigging.PresetAudition
import com.donnclab.kinetiframe.presentation.actors.rigging.PresetAuditionControls
import com.donnclab.kinetiframe.presentation.actors.rigging.PresetLibrarySection
import com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging.ActorMotionTimingMenu
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PresetAuditionTest {
    @get:Rule val compose = createComposeRule()
    private val recipe = PresetRecipeDocument(
        PresetRecipe(
            PresetRecipeMetadata(1, "wave", "Wave", 2f),
            PresetRecipeChannels(listOf("head"), listOf(PresetRotationChannel("head"))),
        ),
        description = "",
    )

    @Test fun darkSheetCardCanSelectAndApply() {
        var selected: String? = null
        var applied = false
        compose.setContent {
            PresetLibrarySection(listOf(ActorPreset(recipe)), null, true, { selected = it }, { applied = true }, true)
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
            PresetAuditionControls(
                PresetAudition(AppliedPresetDocument(recipe), false, true, {
                    played = true
                }, {}, {}, null),
            )
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
