// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetOscillation
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel
import com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppliedPresetProgramTest {
    private val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 100f)))
    private val rotations =
        listOf(PresetRotationChannel("head", points = listOf(RotationPoint(0f, 2f), RotationPoint(1f, 8f))))
    private val roots = listOf(RootMotionChannel("root", listOf(RootMotionPoint(0f), RootMotionPoint(1f, 1f))))
    private val recipe = PresetRecipe(
        PresetRecipeMetadata(3, "test", "Test", 1f),
        PresetRecipeChannels(listOf("root", "head"), rotations, roots),
        false,
    )
    private val document = AppliedPresetDocument(PresetRecipeDocument(recipe, ""))

    @Test fun `schema dispatch does not consume authored fields for legacy recipes`() {
        val legacy = document.copy(
            recipe = document.recipe.copy(recipe = recipe.copy(metadata = recipe.metadata.copy(schemaVersion = 1))),
        )
        assertEquals(mapOf("head" to 0f), AppliedPresetProgram(legacy).offsets(500_000))
        assertEquals(mapOf("head" to 5f), AppliedPresetProgram(document).offsets(500_000))
    }

    @Test fun `legacy dispatch forwards raw speed intensity and loop mode`() {
        val channel = PresetRotationChannel("head", PresetOscillation(30f, 40f, 1f, 90f))
        val legacy = recipe.copy(
            metadata = recipe.metadata.copy(schemaVersion = 1),
            channels = recipe.channels.copy(rotations = listOf(channel)),
        )
        val input = document.copy(recipe = document.recipe.copy(recipe = legacy), speed = 2f, intensity = .5f)
        assertEquals(mapOf("head" to -2.5f), AppliedPresetProgram(input).offsets(250_000))
        val looping = input.copy(recipe = input.recipe.copy(recipe = legacy.copy(loop = true)))
        assertEquals(mapOf("head" to -5f), AppliedPresetProgram(looping).offsets(250_000))
    }

    @Test fun `settings and clocks stay bound to each program without sample state`() {
        val normal = AppliedPresetProgram(document)
        val changed = AppliedPresetProgram(document.copy(speed = 2f, intensity = 2f))
        assertEquals(1_000_000L, normal.durationMicros)
        assertEquals(500_000L, changed.durationMicros)
        assertEquals(mapOf("head" to 10f), changed.offsets(250_000))
        normal.offsets(Long.MAX_VALUE)
        assertEquals(mapOf("head" to 5f), normal.offsets(500_000))
    }

    @Test fun `root only and duration calls never traverse rotation channels`() {
        val inaccessible = object : AbstractList<PresetRotationChannel>() {
            override val size: Int get() = 1
            override fun get(index: Int): PresetRotationChannel = error("Rotation traversal")
        }
        val input = document.copy(
            recipe = document.recipe.copy(
                recipe = recipe.copy(channels = recipe.channels.copy(rotations = inaccessible)),
            ),
        )
        val program = AppliedPresetProgram(input)
        assertEquals(1_000_000L, program.durationMicros)
        assertEquals(
            mapOf("root" to RootTranslation(50f, 0f)),
            program.rootTranslations(500_000, graph, PresetRootMotionSampler()),
        )
    }

    @Test fun `empty roots bypass rotation compilation with raw settings`() {
        val input = document.copy(
            speed = Float.NaN,
            recipe = document.recipe.copy(
                recipe = recipe.copy(channels = recipe.channels.copy(rotations = emptyList(), roots = emptyList())),
            ),
        )
        assertEquals(
            emptyMap<String, RootTranslation>(),
            AppliedPresetProgram(input).rootTranslations(0, graph, PresetRootMotionSampler()),
        )
    }

    @Test fun `caller shared sampler eviction and clear do not change either program`() {
        val sampler = PresetRootMotionSampler(1)
        val first = AppliedPresetProgram(document)
        val changedRoots = roots.map { it.copy(points = listOf(RootMotionPoint(0f), RootMotionPoint(1f, 2f))) }
        val changedRecipe = recipe.copy(channels = recipe.channels.copy(roots = changedRoots))
        val second = AppliedPresetProgram(document.copy(recipe = document.recipe.copy(recipe = changedRecipe)))
        val expected = first.rootTranslations(500_000, graph, sampler)
        assertEquals(mapOf("root" to RootTranslation(100f, 0f)), second.rootTranslations(500_000, graph, sampler))
        assertEquals(expected, first.rootTranslations(500_000, graph, sampler))
        sampler.clear()
        assertEquals(expected, first.rootTranslations(500_000, graph, sampler))
    }
}
