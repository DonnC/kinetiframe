// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import com.donnclab.kinetiframe.core.actor.presets.PresetChannelIssue
import com.donnclab.kinetiframe.core.actor.presets.PresetChannelValidationException
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeIssue
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeValidationException
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationException
import com.donnclab.kinetiframe.core.actor.validation.GraphValidationIssue
import com.donnclab.kinetiframe.presentation.R

/** The wording for a typed failure in this throwable or its causes, or null when there is none. */
fun Throwable.typedFailureText(): UiText? = generateSequence(this) { it.cause }.firstNotNullOfOrNull { failure ->
    when (failure) {
        is GraphValidationException -> failure.issue.text()
        is PresetRecipeValidationException -> failure.issue.text()
        is PresetChannelValidationException -> failure.issue.text()
        else -> null
    }
}

private fun GraphValidationIssue.text(): UiText = when (this) {
    GraphValidationIssue.PART_COUNT -> UiText.Resource(R.string.actor_invalid_part_count)

    GraphValidationIssue.VARIANT_COUNT -> UiText.Resource(R.string.actor_invalid_variant_count)

    GraphValidationIssue.ATTACHMENT_PAIR -> UiText.Resource(R.string.actor_invalid_attachment_pair)

    GraphValidationIssue.IK_ROOT -> UiText.Resource(R.string.actor_invalid_ik_root)

    GraphValidationIssue.HIERARCHY_CYCLE -> UiText.Resource(R.string.actor_invalid_hierarchy_cycle)

    GraphValidationIssue.GRID_BUDGET -> UiText.Resource(R.string.actor_invalid_grid_budget)

    GraphValidationIssue.SINGULAR_BIND -> UiText.Resource(R.string.actor_invalid_singular_bind)

    GraphValidationIssue.SURFACE_DESCENDANTS -> UiText.Resource(R.string.actor_invalid_surface_descendants)

    GraphValidationIssue.SURFACE_OVERLAP -> UiText.Resource(R.string.actor_invalid_surface_overlap)

    // The rest are integrity checks a person can't act on; the name helps a support report.
    else -> UiText.Resource(R.string.actor_invalid_data, listOf(name))
}

private fun PresetRecipeIssue.text(): UiText = when (this) {
    PresetRecipeIssue.UNSUPPORTED_SCHEMA -> UiText.Resource(R.string.preset_newer_version)
    PresetRecipeIssue.ROOT_SCHEMA -> UiText.Resource(R.string.preset_unsupported_feature)
    else -> UiText.Resource(R.string.preset_invalid_data, listOf(name))
}

private fun PresetChannelIssue.text(): UiText = when (this) {
    PresetChannelIssue.LEGACY_AUTHORED_FIELDS -> UiText.Resource(R.string.preset_unsupported_feature)
    PresetChannelIssue.POINT_LIMIT -> UiText.Resource(R.string.preset_invalid_point_count)
    PresetChannelIssue.LOOP_ENDPOINTS -> UiText.Resource(R.string.preset_invalid_loop)
    PresetChannelIssue.INVALID_COMPENSATION -> UiText.Resource(R.string.preset_invalid_compensation)
    else -> UiText.Resource(R.string.preset_invalid_data, listOf(name))
}
