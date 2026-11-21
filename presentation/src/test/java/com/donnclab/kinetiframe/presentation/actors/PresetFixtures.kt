package com.donnclab.kinetiframe.presentation.actors

import com.donnclab.kinetiframe.core.actor.presets.PresetOscillation
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.core.actor.presets.PresetRotationChannel
import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel

/** A recipe for tests, named after its ID. Required bones default to the rotated ones. */
internal fun testRecipe(
    id: String,
    rotations: List<PresetRotationChannel>,
    durationSeconds: Float = 1f,
    loop: Boolean = true,
    requiredBones: List<String> = rotations.map { it.boneId },
    schemaVersion: Int = 1,
) = PresetRecipeDocument(
    PresetRecipe(
        PresetRecipeMetadata(schemaVersion, id, id, durationSeconds),
        PresetRecipeChannels(requiredBones, rotations),
        loop,
    ),
    description = "",
)

internal fun PresetRecipeDocument.withRootMotion(roots: List<RootMotionChannel>) =
    copy(recipe = recipe.copy(channels = recipe.channels.copy(roots = roots)))

/** A legacy schema 1 channel. */
internal fun oscillating(
    boneId: String,
    offset: Float = 0f,
    amplitude: Float = 0f,
    cycles: Float = 1f,
    phase: Float = 0f,
) = PresetRotationChannel(boneId, PresetOscillation(offset, amplitude, cycles, phase))
