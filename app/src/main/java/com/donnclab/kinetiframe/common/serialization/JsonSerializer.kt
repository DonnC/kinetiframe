package com.donnclab.kinetiframe.common.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Utility for JSON serialization and deserialization using kotlinx.serialization.
 */
object JsonSerializer {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    /**
     * Serializes an object to a JSON string.
     */
    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    /**
     * Deserializes a JSON string to an object.
     */
    inline fun <reified T> decode(string: String): T = json.decodeFromString(string)
}
