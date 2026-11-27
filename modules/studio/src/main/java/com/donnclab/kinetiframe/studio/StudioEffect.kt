package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.core.mvi.ViewEffect

/**
 * One-off events the studio shows once, such as an error or a text dialog.
 */
sealed class StudioEffect : ViewEffect {
    /**
     * A problem to show. Screens word [failure] from their own string resources. A typed [cause]
     * takes precedence, and [detail] fills in failures whose wording names something, such as the
     * missing parts a preset needs.
     */
    data class ShowError(val failure: StudioFailure, val cause: Throwable? = null, val detail: String? = null) :
        StudioEffect()

    data class ShowTextEditDialog(val id: String, val initialText: String) : StudioEffect()
    data class ShowTextAddDialog(val x: Float, val y: Float) : StudioEffect()
}

/** Problems the studio reports. */
enum class StudioFailure {
    // Limits and steps the person can act on.
    FRAME_LIMIT,
    ARTWORK_KEY_LIMIT,
    TAKE_TOO_LONG,
    RECORDING_TOO_LATE,
    APPLY_PRESET_BEFORE_RECORDING,
    REMOVE_PRESET_BEFORE_POSING,
    PRESET_FOR_ANOTHER_VIEW,

    /** [StudioEffect.ShowError.detail] lists the missing parts. */
    PRESET_MISSING_PARTS,
    SELECT_PART_FOR_CUTOUT,

    // Operations that failed. A lower layer's message may come with the cause.
    PLAYBACK_STOPPED,
    PRESETS_NOT_LOADED,
    CAMERA_EDIT_FAILED,
    HOLD_UPDATE_FAILED,
    BACKGROUND_OUT_OF_MEMORY,
    BACKGROUND_NOT_LOADED,
    BACKGROUND_NOT_SAVED,
    DRAWING_NOT_BOUND,
    BLEND_FAILED,
    ARTWORK_EDIT_FAILED,
    ARTWORK_NOT_ATTACHED,
    SAMPLE_ARTWORK_FAILED,
    CUTOUT_FAILED,
}
