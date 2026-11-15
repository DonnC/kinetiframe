package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetChannelIssue
import com.donnclab.kinetiframe.core.actor.presets.PresetChannelValidationException
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeIssue
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeValidationException

/**
 * The messages the legacy references throw, rebuilt from a typed failure, so the parity tests can
 * keep checking which issue is reported first. The app words issues from string resources.
 */
internal object LegacyPresetValidationMessages {
    private const val REQUIREMENT_FAILURE = "Failed requirement."

    fun of(failure: Throwable?, schema: Int): String? = when (failure) {
        null -> null
        is PresetRecipeValidationException -> recipe(failure.issue, schema)
        is PresetChannelValidationException -> channel(failure.issue)
        else -> failure.message
    }

    private fun channel(issue: PresetChannelIssue): String = when (issue) {
        PresetChannelIssue.LEGACY_AUTHORED_FIELDS -> "Authored curves require schema 2"

        PresetChannelIssue.POINT_LIMIT -> "A curve needs 2..64 ordered points"

        PresetChannelIssue.LOOP_ENDPOINTS -> "Loop endpoints must match"

        PresetChannelIssue.INVALID_COMPENSATION ->
            "Counter-rotation must reference uncompensated channels from this recipe"

        else -> REQUIREMENT_FAILURE
    }

    private fun recipe(issue: PresetRecipeIssue, schema: Int): String = when (issue) {
        PresetRecipeIssue.UNSUPPORTED_SCHEMA -> "Unsupported motion preset schema: $schema"
        PresetRecipeIssue.ROOT_SCHEMA -> "Root motion requires preset schema 3"
        else -> REQUIREMENT_FAILURE
    }
}
