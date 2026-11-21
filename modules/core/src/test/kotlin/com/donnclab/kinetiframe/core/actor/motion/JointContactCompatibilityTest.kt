// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class JointContactCompatibilityTest {
    private val full = """{"x":12.0,"y":20.0,"bendSide":-1,"followBoneId":"foot","followWorldAngle":75.0}"""
    private val minimal = """{"x":12.0,"y":20.0,"bendSide":1}"""
    private val defaults = Json { encodeDefaults = true }
    private val permissive = Json { ignoreUnknownKeys = true }

    @Test fun `legacy contact fields remain flat and byte equivalent`() {
        val value = Json.decodeFromString<JointContact>(full)
        assertEquals(Point2(12f, 20f), value.position)
        assertSame(ContactBend.Negative, value.bend)
        assertEquals(ContactFollow("foot", 75f), value.follow)
        assertEquals(full, Json.encodeToString(value))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.JointContact",
            JointContact.serializer().descriptor.serialName,
        )
    }

    @Test fun `minimal and explicit default encodings retain five original keys`() {
        val value = Json.decodeFromString<JointContact>(minimal)
        assertEquals(JointContact(12f, 20f, 1), value)
        assertEquals(minimal, Json.encodeToString(value))
        val output = defaults.encodeToString(value)
        assertEquals(
            setOf("x", "y", "bendSide", "followBoneId", "followWorldAngle"),
            Json.parseToJsonElement(output).jsonObject.keys,
        )
        assertEquals(value, Json.decodeFromString<JointContact>(output))
    }

    @Test fun `required coordinates and sign cannot be missing or null`() {
        val fields = Json.parseToJsonElement(minimal).jsonObject
        fields.keys.forEach { key ->
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<JointContact>(JsonObject(fields - key).toString())
            }
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<JointContact>(JsonObject(fields + (key to JsonNull)).toString())
            }
        }
    }

    @Test fun `unknown fields follow caller policy and invalid signs are not normalized`() {
        val future = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<JointContact>(future) }
        assertEquals(Json.decodeFromString<JointContact>(minimal), permissive.decodeFromString<JointContact>(future))
        val invalid = Json.decodeFromString<JointContact>(minimal.replace("\"bendSide\":1", "\"bendSide\":0"))
        assertEquals(ContactBend.Unrecognized(0), invalid.bend)
        assertEquals(0, invalid.bendSide)
        assertEquals(invalid, Json.decodeFromString<JointContact>(Json.encodeToString(invalid)))
    }

    @Test fun `bend edits preserve position follow and original record`() {
        val value = Json.decodeFromString<JointContact>(full)
        val changed = value.withBendSide(1)
        assertSame(value.position, changed.position)
        assertSame(value.follow, changed.follow)
        assertEquals(-1, value.bendSide)
        assertSame(ContactBend.Positive, changed.bend)
    }
}
