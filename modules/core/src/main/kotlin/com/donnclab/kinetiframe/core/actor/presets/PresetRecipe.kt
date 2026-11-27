// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

data class PresetRecipeMetadata(val schemaVersion: Int, val id: String, val name: String, val durationSeconds: Float)

data class PresetRecipeChannels(
    val requiredBones: List<String>,
    val rotations: List<PresetRotationChannel>,
    val roots: List<RootMotionChannel> = emptyList(),
)

data class PresetRecipe(
    val metadata: PresetRecipeMetadata,
    val channels: PresetRecipeChannels,
    val loop: Boolean = true,
)
