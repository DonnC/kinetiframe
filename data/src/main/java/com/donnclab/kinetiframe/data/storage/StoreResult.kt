// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.data.storage

/** The outcome of a store operation: its value, or a message the UI can show. */
sealed interface StoreResult<out T> {
    data class Value<T>(val value: T) : StoreResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : StoreResult<Nothing>
}
