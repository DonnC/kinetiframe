package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.actors.domain.ActorTemplateContract
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface ActorStoreResult<out T> {
    data class Value<T>(val value: T) : ActorStoreResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : ActorStoreResult<Nothing>
}
