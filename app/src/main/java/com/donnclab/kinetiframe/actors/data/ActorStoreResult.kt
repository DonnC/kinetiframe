package com.donnclab.kinetiframe.actors.data

sealed interface ActorStoreResult<out T> {
    data class Value<T>(val value: T) : ActorStoreResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : ActorStoreResult<Nothing>
}
