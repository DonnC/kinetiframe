// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AppliedPresetDocumentWireTest {
    private val defaults = Json { encodeDefaults = true }
    private val tolerant = Json { ignoreUnknownKeys = true }
    private val recipe =
        """{"id":"test","name":"Test","description":"","durationSeconds":1.0,"requiredBones":["head"],""" +
            """"channels":[{"boneId":"head"}]}"""

    @Test fun `application retains required snapshot and optional settings`() {
        val minimal = """{"recipe":$recipe}"""
        val value = Json.decodeFromString<AppliedPresetDocument>(minimal)
        assertEquals(1f, value.intensity)
        assertEquals(1f, value.speed)
        assertEquals(minimal, Json.encodeToString(value))
        assertEquals(
            """{"recipe":${defaults.encodeToString(value.recipe)},"intensity":1.0,"speed":1.0}""",
            defaults.encodeToString(value),
        )
        assertThrows(SerializationException::class.java) { Json.decodeFromString<AppliedPresetDocument>("{}") }
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<AppliedPresetDocument>("""{"recipe":null}""")
        }
    }

    @Test fun `raw settings including signed zero survive without clamping`() {
        val wire = """{"recipe":$recipe,"intensity":-0.0,"speed":9.0}"""
        val value = Json.decodeFromString<AppliedPresetDocument>(wire)
        assertEquals((-0f).toBits(), value.intensity.toBits())
        assertEquals(9f, value.speed)
        assertEquals(wire, Json.encodeToString(value))
    }

    @Test fun `unknown and null settings remain decoder policy not implicit repair`() {
        val future = """{"recipe":$recipe,"future":true}"""
        assertThrows(SerializationException::class.java) { Json.decodeFromString<AppliedPresetDocument>(future) }
        assertEquals(
            """{"recipe":$recipe}""",
            Json.encodeToString(tolerant.decodeFromString<AppliedPresetDocument>(future)),
        )
        listOf("intensity", "speed").forEach { field ->
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<AppliedPresetDocument>("""{"recipe":$recipe,"$field":null}""")
            }
        }
    }
}
