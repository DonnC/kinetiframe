// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio

import androidx.annotation.StringRes
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.typedFailureText
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioFailure

/** Failures whose wording names a reason or the parts they need, from [StudioEffect.ShowError.detail]. */
private val DETAIL_FAILURES = setOf(
    StudioFailure.PRESET_MISSING_PARTS,
    StudioFailure.PLAYBACK_STOPPED,
    StudioFailure.PRESETS_NOT_LOADED,
    StudioFailure.BACKGROUND_NOT_SAVED,
    StudioFailure.SAMPLE_ARTWORK_FAILED,
)

/**
 * The wording for a studio error: a typed cause's wording first, then the failure's own text. For
 * failures without a detail, a lower layer's message still comes first while that layer has no
 * typed errors.
 */
internal fun StudioEffect.ShowError.toUiText(): UiText {
    cause?.typedFailureText()?.let { return it }
    val text = failure.textRes()
    if (failure in DETAIL_FAILURES) return UiText.Resource(text, listOf(detail.orEmpty()))
    return cause?.message?.let(UiText::Verbatim) ?: UiText.Resource(text)
}

@StringRes
private fun StudioFailure.textRes(): Int = when (this) {
    StudioFailure.FRAME_LIMIT -> R.string.studio_error_frame_limit
    StudioFailure.ARTWORK_KEY_LIMIT -> R.string.studio_error_artwork_key_limit
    StudioFailure.TAKE_TOO_LONG -> R.string.studio_error_take_too_long
    StudioFailure.RECORDING_TOO_LATE -> R.string.studio_error_recording_too_late
    StudioFailure.APPLY_PRESET_BEFORE_RECORDING -> R.string.studio_error_apply_preset_first
    StudioFailure.REMOVE_PRESET_BEFORE_POSING -> R.string.studio_error_remove_preset_first
    StudioFailure.PRESET_FOR_ANOTHER_VIEW -> R.string.studio_error_preset_other_view
    StudioFailure.PRESET_MISSING_PARTS -> R.string.studio_error_preset_missing_parts
    StudioFailure.SELECT_PART_FOR_CUTOUT -> R.string.studio_error_select_part_for_cutout
    StudioFailure.PLAYBACK_STOPPED -> R.string.studio_error_playback_stopped
    StudioFailure.PRESETS_NOT_LOADED -> R.string.studio_error_presets_not_loaded
    StudioFailure.CAMERA_EDIT_FAILED -> R.string.studio_error_camera_edit
    StudioFailure.HOLD_UPDATE_FAILED -> R.string.studio_error_hold_update
    StudioFailure.BACKGROUND_OUT_OF_MEMORY -> R.string.studio_error_background_memory
    StudioFailure.BACKGROUND_NOT_LOADED -> R.string.studio_error_background_load
    StudioFailure.BACKGROUND_NOT_SAVED -> R.string.studio_error_background_save
    StudioFailure.DRAWING_NOT_BOUND -> R.string.studio_error_bind_drawing
    StudioFailure.BLEND_FAILED -> R.string.studio_error_blend
    StudioFailure.ARTWORK_EDIT_FAILED -> R.string.studio_error_edit_artwork
    StudioFailure.ARTWORK_NOT_ATTACHED -> R.string.studio_error_attach_artwork
    StudioFailure.SAMPLE_ARTWORK_FAILED -> R.string.studio_error_sample_artwork
    StudioFailure.CUTOUT_FAILED -> R.string.studio_error_cutout
}
