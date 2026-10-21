// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresetRecipeDocumentWireTest {
    private val defaults = Json { encodeDefaults = true }
    private val tolerant = Json { ignoreUnknownKeys = true }
    private val minimal =
        """{"id":"test","name":"Test","description":"","durationSeconds":1.0,"requiredBones":["head"],""" +
            """"channels":[{"boneId":"head"}]}"""

    @Test fun `minimal recipe retains flat fields and defaults`() {
        val value = Json.decodeFromString<PresetRecipeDocument>(minimal)
        assertEquals(minimal, Json.encodeToString(value))
        assertEquals(1, value.recipe.metadata.schemaVersion)
        assertTrue(value.recipe.loop)
        assertEquals(PresetAuthoredView.FRONT, value.authoredView)
        val expanded = defaults.encodeToString(value)
        assertEquals(
            """{"schemaVersion":1,"id":"test","name":"Test","description":"","durationSeconds":1.0,"requiredBones":["head"],"channels":[{"boneId":"head","offsetDegrees":0.0,"amplitudeDegrees":0.0,"cyclesPerLoop":1.0,"phaseDegrees":0.0,"points":[],"counterRotationBones":[]}],"loop":true,"rootMotion":[],"authoredView":"FRONT"}""",
            expanded,
        )
    }

    @Test fun `every mandatory field remains required and null is not a default`() {
        val fields = Json.parseToJsonElement(minimal).jsonObject
        fields.keys.forEach { key ->
            val absent = fields.filterKeys { it != key }
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<PresetRecipeDocument>(JsonObject(absent).toString())
            }
        }
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<PresetRecipeDocument>(minimal.replace("\"description\":\"\"", "\"description\":null"))
        }
    }

    @Test fun `unknown fields remain caller policy and unsupported views fail`() {
        val extended = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<PresetRecipeDocument>(extended) }
        assertEquals(minimal, Json.encodeToString(tolerant.decodeFromString<PresetRecipeDocument>(extended)))
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<PresetRecipeDocument>(minimal.dropLast(1) + ",\"authoredView\":\"FUTURE\"}")
        }
    }

    @Test fun `decoding preserves invalid future schemas without implicit validation`() {
        val wire =
            """{"schemaVersion":99,"id":"","name":"","description":"","durationSeconds":-1.0,""" +
                """"requiredBones":[],"channels":[],"loop":false,"authoredView":"BACK"}"""
        val value = Json.decodeFromString<PresetRecipeDocument>(wire)
        assertEquals(wire, Json.encodeToString(value))
        assertThrows(PresetRecipeValidationException::class.java) { PresetRecipeValidation.requireValid(value.recipe) }
    }

    @Test fun `view compatibility retains original wildcard without treating front as universal`() {
        val document = Json.decodeFromString<PresetRecipeDocument>(minimal)
        PresetAuthoredView.entries.forEach { authored ->
            val value = document.copy(authoredView = authored)
            PresetAuthoredView.entries.forEach { requested ->
                assertEquals(
                    requested == PresetAuthoredView.ORIGINAL || requested == authored,
                    value.supportsView(requested),
                )
            }
        }
        assertFalse(document.supportsView(PresetAuthoredView.PROFILE))
    }
}
