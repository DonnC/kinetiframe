// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts

enum class ServiceFailure { UNAVAILABLE, UNAUTHORIZED, NOT_FOUND, INVALID_DATA, QUOTA_EXCEEDED, RETRYABLE_IO }
enum class ServiceAvailability { AVAILABLE, UNAVAILABLE, REAUTHENTICATION_REQUIRED }

/** Cancellation is never mapped to Failure; adapters must propagate CancellationException. */
sealed interface ServiceResult<out T> {
    data class Value<T>(val value: T) : ServiceResult<T>
    data class Failure(val reason: ServiceFailure) : ServiceResult<Nothing>
}
