// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

object PresetRecipeValidation {
    private val recipeId = Regex("[a-z0-9_-]{1,80}")

    fun requireValid(recipe: PresetRecipe): PresetRecipe = recipe.apply {
        val metadata = recipe.metadata
        val channels = recipe.channels
        requireRecipe(metadata.schemaVersion in MIN_SCHEMA..MAX_SCHEMA, PresetRecipeIssue.UNSUPPORTED_SCHEMA)
        requireRecipe(metadata.id.matches(recipeId) && metadata.name.isNotBlank(), PresetRecipeIssue.INVALID_IDENTITY)
        requireRecipe(
            metadata.durationSeconds.isFinite() && metadata.durationSeconds in MIN_DURATION..MAX_DURATION,
            PresetRecipeIssue.INVALID_DURATION,
        )
        requireRecipe(
            channels.rotations.isNotEmpty() && channels.rotations.size <= MAX_ROTATIONS,
            PresetRecipeIssue.ROTATION_COUNT,
        )
        requireRecipe(
            channels.rotations.map {
                it.boneId
            }.distinct().size == channels.rotations.size,
            PresetRecipeIssue.DUPLICATE_ROTATIONS,
        )
        requireRecipe(
            channels.requiredBones.isNotEmpty() &&
                channels.requiredBones.distinct().size == channels.requiredBones.size,
            PresetRecipeIssue.INVALID_REQUIRED_BONES,
        )
        requireRecipe(metadata.schemaVersion == ROOT_SCHEMA || channels.roots.isEmpty(), PresetRecipeIssue.ROOT_SCHEMA)
        PresetRootValidation.requireValid(channels.roots, channels.requiredBones, recipe.loop)
        // Preserve root-before-rotation failure precedence at the import/publication boundary.
        PresetChannelValidation.requireValid(channels.rotations, metadata.schemaVersion, recipe.loop)
    }

    private const val MIN_SCHEMA = 1
    private const val MAX_SCHEMA = 3
    private const val ROOT_SCHEMA = 3
    private const val MIN_DURATION = .25f
    private const val MAX_DURATION = 30f
    private const val MAX_ROTATIONS = 128
}

enum class PresetRecipeIssue {
    UNSUPPORTED_SCHEMA,
    INVALID_IDENTITY,
    INVALID_DURATION,
    ROTATION_COUNT,
    DUPLICATE_ROTATIONS,
    INVALID_REQUIRED_BONES,
    ROOT_SCHEMA,
    ROOT_COUNT,
    INVALID_ROOT_CHANNEL,
    INVALID_ROOT_ENDPOINTS,
    INVALID_ROOT_POINT,
    UNORDERED_ROOT_POINTS,
    ROOT_LOOP_ENDPOINTS,
}

class PresetRecipeValidationException(val issue: PresetRecipeIssue) : IllegalArgumentException(issue.name)

internal fun requireRecipe(valid: Boolean, issue: PresetRecipeIssue) {
    if (!valid) throw PresetRecipeValidationException(issue)
}
