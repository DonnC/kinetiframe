package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import com.donnclab.kinetiframe.core.document.StageDimensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.round

class AppliedPresetPlaybackTest {
    private fun recipe(durationSeconds: Float = 1.7f) = PresetRecipeDocument(
        PresetRecipe(
            PresetRecipeMetadata(2, "test", "Test", durationSeconds),
            PresetRecipeChannels(
                listOf("head"),
                listOf(PresetRotationChannel("head", points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 30f)))),
            ),
        ),
        description = "",
    )

    @Test fun `duration keeps the frozen formula and clamps only the evaluation speed`() {
        listOf(-1f, 0f, .25f, 1.7f, 30f, Float.MAX_VALUE, Float.POSITIVE_INFINITY, Float.NaN).forEach { duration ->
            listOf(-1f, .25f, 1f, 1.3f, 3f, 9f, Float.NaN).forEach { speed ->
                val applied = AppliedPresetDocument(recipe(duration), speed = speed)
                val expected = round(
                    duration.toDouble() * 1_000_000 / speed.coerceIn(.25f, 3f),
                ).toLong().coerceAtLeast(1)
                assertEquals(expected, applied.durationMicros)
            }
        }
    }

    @Test fun `equal documents evaluate alike whether or not they share a cached program`() {
        val first = AppliedPresetDocument(recipe(), intensity = .5f)
        val second = first.copy()
        listOf(0L, 400_000L, 850_000L, 1_700_000L).forEach { time ->
            assertEquals(first.offsets(time), second.offsets(time))
            assertEquals(first.offsets(time), first.offsets(time))
        }
        assertTrue(first.offsets(850_000L).getValue("head") > 0f)
    }

    @Test fun `a recipe without root motion translates nothing`() {
        val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
        assertEquals(emptyMap<String, Any>(), AppliedPresetDocument(recipe()).rootTranslations(500_000L, rest))
    }
}
