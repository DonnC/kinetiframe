package com.donnclab.kinetiframe.data.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class ProjectArchivePaths(
    val sourceDirectory: String,
    val externalAssets: Map<String, String> = emptyMap(),
) {
    /** Only rewrite strings belonging to the archived project, never arbitrary labels or URIs. */
    fun relocate(element: JsonElement, destination: String): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { relocate(it.value, destination) })

        is JsonArray -> JsonArray(element.map { relocate(it, destination) })

        is JsonPrimitive -> if (!element.isString) {
            element
        } else {
            val original = element.content
            val normalized = original.replace('\\', '/')
            val source = sourceDirectory.replace('\\', '/').trimEnd('/')
            val suffix =
                externalAssets[original] ?: normalized.takeIf { it.startsWith("$source/") }?.removePrefix("$source/")
            if (suffix == null) element else JsonPrimitive("${destination.replace('\\', '/').trimEnd('/')}/$suffix")
        }
    }
}
