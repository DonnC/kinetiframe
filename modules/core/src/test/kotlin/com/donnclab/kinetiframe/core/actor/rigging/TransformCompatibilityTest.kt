// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class TransformCompatibilityTest {
    private val defaults = Json { encodeDefaults = true }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }
    private val full = """{"x":2.0,"y":3.0,"rotationDegrees":90.0,"scaleX":-2.0,"scaleY":4.0}"""

    @Test fun `legacy flat transform retains nonuniform scale reflection and identity`() {
        val value = Json.decodeFromString<Transform>(full)
        assertEquals(Transform(Point2(2f, 3f), 90f, Point2(-2f, 4f)), value)
        assertEquals(full, Json.encodeToString(value))
        assertEquals("com.kinetiframe.engine.rigging.model.Transform", Transform.serializer().descriptor.serialName)
    }

    @Test fun `empty and partial archives preserve independent defaults`() {
        assertEquals(Transform(), Json.decodeFromString<Transform>("{}"))
        assertEquals("{}", Json.encodeToString(Transform()))
        assertEquals(
            """{"x":0.0,"y":0.0,"rotationDegrees":0.0,"scaleX":1.0,"scaleY":1.0}""",
            defaults.encodeToString(Transform()),
        )
        val partial = Json.decodeFromString<Transform>("""{"y":8.0,"scaleY":2.0}""")
        assertEquals(0f, partial.x)
        assertEquals(8f, partial.y)
        assertEquals(1f, partial.scaleX)
        assertEquals(2f, partial.scaleY)
    }

    @Test fun `unknown fields and nulls follow caller decoding policy`() {
        val future = full.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<Transform>(future) }
        assertEquals(Json.decodeFromString<Transform>(full), forwardCompatible.decodeFromString<Transform>(future))
        assertThrows(SerializationException::class.java) { Json.decodeFromString<Transform>("""{"x":null}""") }
    }

    @Test fun `position edits preserve orientation stretch and original value`() {
        val value = Json.decodeFromString<Transform>(full)
        val edited = value.withPosition(y = 7f)
        assertEquals(Point2(2f, 7f), edited.translation)
        assertEquals(value.rotationDegrees, edited.rotationDegrees)
        assertEquals(value.stretch, edited.stretch)
        assertEquals(Point2(2f, 3f), value.translation)
        assertEquals(Transform(2f, 3f, 90f), Transform(Point2(2f, 3f), 90f))
    }

    @Test fun `raw zero scale remains decodable for graph validation`() {
        assertEquals(0f, Json.decodeFromString<Transform>("""{"scaleX":0.0}""").scaleX)
    }
}
