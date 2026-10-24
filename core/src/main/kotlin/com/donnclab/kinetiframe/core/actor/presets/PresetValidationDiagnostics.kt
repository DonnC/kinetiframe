// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

/** Messages for preset validation failures, as saved files and the preset library report them. */
object PresetValidationDiagnostics {
    private const val REQUIREMENT_FAILURE = "Failed requirement."

    /** Throws [IllegalArgumentException] with the matching message when [recipe] is invalid. */
    fun requireValidRecipe(recipe: PresetRecipe) {
        try {
            PresetRecipeValidation.requireValid(recipe)
        } catch (failure: PresetRecipeValidationException) {
            throw IllegalArgumentException(recipe(failure.issue, recipe.metadata.schemaVersion))
        } catch (failure: PresetChannelValidationException) {
            throw IllegalArgumentException(channel(failure.issue))
        }
    }

    fun channel(issue: PresetChannelIssue): String = when (issue) {
        PresetChannelIssue.LEGACY_AUTHORED_FIELDS -> "Authored curves require schema 2"

        PresetChannelIssue.POINT_LIMIT -> "A curve needs 2..64 ordered points"

        PresetChannelIssue.LOOP_ENDPOINTS -> "Loop endpoints must match"

        PresetChannelIssue.INVALID_COMPENSATION ->
            "Counter-rotation must reference uncompensated channels from this recipe"

        PresetChannelIssue.EMPTY_PART_ID, PresetChannelIssue.NONFINITE_CONTROLS,
        PresetChannelIssue.INVALID_OSCILLATION, PresetChannelIssue.INVALID_PHASE_OFFSET,
        PresetChannelIssue.INVALID_ENDPOINTS, PresetChannelIssue.INVALID_POINT,
        PresetChannelIssue.UNORDERED_POINTS, PresetChannelIssue.DUPLICATE_COMPENSATION,
        -> REQUIREMENT_FAILURE
    }

    fun recipe(issue: PresetRecipeIssue, schema: Int): String = when (issue) {
        PresetRecipeIssue.UNSUPPORTED_SCHEMA -> "Unsupported motion preset schema: $schema"

        PresetRecipeIssue.ROOT_SCHEMA -> "Root motion requires preset schema 3"

        PresetRecipeIssue.INVALID_IDENTITY, PresetRecipeIssue.INVALID_DURATION,
        PresetRecipeIssue.ROTATION_COUNT, PresetRecipeIssue.DUPLICATE_ROTATIONS,
        PresetRecipeIssue.INVALID_REQUIRED_BONES, PresetRecipeIssue.ROOT_COUNT,
        PresetRecipeIssue.INVALID_ROOT_CHANNEL, PresetRecipeIssue.INVALID_ROOT_ENDPOINTS,
        PresetRecipeIssue.INVALID_ROOT_POINT, PresetRecipeIssue.UNORDERED_ROOT_POINTS,
        PresetRecipeIssue.ROOT_LOOP_ENDPOINTS,
        -> REQUIREMENT_FAILURE
    }
}
