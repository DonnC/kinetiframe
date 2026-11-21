// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.annotation.StringRes
import com.donnclab.kinetiframe.core.contracts.StoreFailure
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.presentation.R

/**
 * The wording for a store failure: a typed cause's wording first, then the failed operation,
 * followed by the cause's own message while its layer has no typed errors.
 */
fun StoreResult.Failure.toUiText(): UiText {
    cause?.typedFailureText()?.let { return it }
    val operation = UiText.Resource(reason.textRes())
    val detail = cause?.message ?: return operation
    return UiText.Resource(R.string.store_failure_detail, listOf(operation, detail))
}

@StringRes
private fun StoreFailure.textRes(): Int = when (this) {
    StoreFailure.PROJECT_STORAGE -> R.string.store_project_storage
    StoreFailure.PROJECT_NOT_CREATED -> R.string.store_project_not_created
    StoreFailure.PROJECT_NOT_RENAMED -> R.string.store_project_not_renamed
    StoreFailure.PROJECT_NOT_IMPORTED -> R.string.store_project_not_imported
    StoreFailure.PROJECT_NOT_ADDED_TO_LIBRARY -> R.string.store_project_not_added
    StoreFailure.PROJECT_ARCHIVE -> R.string.store_project_archive
    StoreFailure.FRAME_PIXELS_NOT_SAVED -> R.string.store_frame_pixels
    StoreFailure.FRAME_DRAFT_NOT_OPENED -> R.string.store_frame_draft
    StoreFailure.ACTOR_STORAGE -> R.string.store_actor_storage
    StoreFailure.ACTOR_OPERATION -> R.string.store_actor_operation
    StoreFailure.PERFORMANCE_STORAGE -> R.string.store_performance_storage
    StoreFailure.PERFORMANCE_NOT_SAVED -> R.string.store_performance_not_saved
    StoreFailure.PLACEMENT_STORAGE -> R.string.store_placement_storage
    StoreFailure.PLACEMENT_INACTIVE -> R.string.store_placement_inactive
    StoreFailure.LIBRARY_STORAGE -> R.string.store_library_storage
    StoreFailure.LIBRARY_NOT_ADDED -> R.string.store_library_not_added
    StoreFailure.LIBRARY_CLEANUP -> R.string.store_library_cleanup
    StoreFailure.PACK_NOT_OPENED -> R.string.store_pack_not_opened
    StoreFailure.PACK_NOT_SAVED -> R.string.store_pack_not_saved
    StoreFailure.PACK_ARCHIVE -> R.string.store_pack_archive
    StoreFailure.EXPORT_FAILED -> R.string.store_export_failed
    StoreFailure.GALLERY_NOT_SAVED -> R.string.store_gallery_not_saved
}
