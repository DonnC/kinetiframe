// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts

/** The outcome of a store operation: its value, or why it failed. */
sealed interface StoreResult<out T> {
    data class Value<T>(val value: T) : StoreResult<T>

    /**
     * [reason] names the operation that failed; screens word it from their own string resources.
     * [cause] carries the underlying error, whose message gives detail while its layer has no
     * typed errors.
     */
    data class Failure(val reason: StoreFailure, val cause: Throwable? = null) : StoreResult<Nothing> {
        /** For logs and for rethrowing inside lower layers. Not for showing to a person. */
        val message: String get() = cause?.message ?: reason.name
    }
}

/** Store operations that can fail, as [StoreResult.Failure] reports them. */
enum class StoreFailure {
    PROJECT_STORAGE,
    PROJECT_NOT_CREATED,
    PROJECT_NOT_RENAMED,
    PROJECT_NOT_IMPORTED,
    PROJECT_NOT_ADDED_TO_LIBRARY,
    PROJECT_ARCHIVE,
    FRAME_PIXELS_NOT_SAVED,
    FRAME_DRAFT_NOT_OPENED,
    ACTOR_STORAGE,
    ACTOR_OPERATION,
    PERFORMANCE_STORAGE,
    PERFORMANCE_NOT_SAVED,
    PLACEMENT_STORAGE,
    PLACEMENT_INACTIVE,
    LIBRARY_STORAGE,
    LIBRARY_NOT_ADDED,
    LIBRARY_CLEANUP,
    PACK_NOT_OPENED,
    PACK_NOT_SAVED,
    PACK_ARCHIVE,
    EXPORT_FAILED,
    GALLERY_NOT_SAVED,
}
