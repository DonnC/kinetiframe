// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class PresetPointsWireTest {
    private val defaults = Json { encodeDefaults = true }
    private val tolerant = Json { ignoreUnknownKeys = true }

    @Test fun `rotation point retains wire identity required fields and order`() {
        val source = """{"progress":0.5,"degrees":-25.0}"""
        val value = Json.decodeFromString<RotationPoint>(source)
        assertEquals(RotationPoint(.5f, -25f), value)
        assertEquals(source, Json.encodeToString(value))
        assertEquals(
            "com.kinetiframe.engine.rigging.presets.RotationPoint",
            RotationPoint.serializer().descriptor.serialName,
        )
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<RotationPoint>("""{"progress":0.0}""")
        }
    }

    @Test fun `root defaults and nested channel preserve legacy wire identity`() {
        val source = """{"boneId":"root","points":[{"progress":0.0}]}"""
        val value = Json.decodeFromString<RootMotionChannel>(source)
        assertEquals(RootMotionChannel("root", listOf(RootMotionPoint(0f))), value)
        assertEquals(source, Json.encodeToString(value))
        assertEquals(
            """{"boneId":"root","points":[{"progress":0.0,"x":0.0,"y":0.0}]}""",
            defaults.encodeToString(value),
        )
        assertEquals(
            "com.kinetiframe.engine.rigging.presets.RootMotionChannel",
            RootMotionChannel.serializer().descriptor.serialName,
        )
        assertEquals(
            "com.kinetiframe.engine.rigging.presets.RootMotionPoint",
            RootMotionPoint.serializer().descriptor.serialName,
        )
    }

    @Test fun `required fields and unknown field policy stay caller owned`() {
        assertThrows(SerializationException::class.java) { Json.decodeFromString<RootMotionPoint>("{}") }
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<RootMotionChannel>("""{"boneId":"root"}""")
        }
        val source = """{"progress":0.0,"future":1}"""
        assertThrows(SerializationException::class.java) { Json.decodeFromString<RootMotionPoint>(source) }
        assertEquals(RootMotionPoint(0f), tolerant.decodeFromString<RootMotionPoint>(source))
    }
}
