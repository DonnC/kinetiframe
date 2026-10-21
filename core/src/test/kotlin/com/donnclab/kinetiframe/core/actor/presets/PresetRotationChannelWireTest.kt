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
class PresetRotationChannelWireTest {
    private val defaults = Json { encodeDefaults = true }
    private val tolerant = Json { ignoreUnknownKeys = true }

    @Test fun `flat fields retain legacy order defaults descriptor and required part id`() {
        val minimal = """{"boneId":"head"}"""
        val full =
            """{"boneId":"head","offsetDegrees":0.0,"amplitudeDegrees":0.0,"cyclesPerLoop":1.0,""" +
                """"phaseDegrees":0.0,"points":[],"counterRotationBones":[]}"""
        val value = Json.decodeFromString<PresetRotationChannel>(minimal)
        assertEquals(PresetRotationChannel("head"), value)
        assertEquals(minimal, Json.encodeToString(value))
        assertEquals(full, defaults.encodeToString(value))
        val descriptor = PresetRotationChannel.serializer().descriptor
        assertEquals("com.kinetiframe.engine.rigging.presets.RotationChannel", descriptor.serialName)
        assertEquals(
            listOf(
                "boneId",
                "offsetDegrees",
                "amplitudeDegrees",
                "cyclesPerLoop",
                "phaseDegrees",
                "points",
                "counterRotationBones",
            ),
            (0 until descriptor.elementsCount).map { descriptor.getElementName(it) },
        )
        assertThrows(SerializationException::class.java) { Json.decodeFromString<PresetRotationChannel>("{}") }
    }

    @Test fun `authored and oscillator data roundtrip together without validation or grouping fields`() {
        val source =
            """{"boneId":"head","offsetDegrees":12.0,"amplitudeDegrees":-7.0,"cyclesPerLoop":0.5,""" +
                """"phaseDegrees":90.0,"points":[{"progress":0.0,"degrees":2.0}],""" +
                """"counterRotationBones":["missing"]}"""
        val value = Json.decodeFromString<PresetRotationChannel>(source)
        assertEquals(PresetOscillation(12f, -7f, .5f, 90f), value.oscillation)
        assertEquals(source, Json.encodeToString(value))
    }

    @Test fun `unknown and null fields follow caller decoding policy`() {
        val future = """{"boneId":"head","future":true}"""
        assertThrows(SerializationException::class.java) { Json.decodeFromString<PresetRotationChannel>(future) }
        assertEquals(PresetRotationChannel("head"), tolerant.decodeFromString<PresetRotationChannel>(future))
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<PresetRotationChannel>("""{"boneId":null}""")
        }
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<PresetRotationChannel>("""{"boneId":"head","points":null}""")
        }
    }

    @Test fun `signed zero remains explicitly encoded like the former generated codec`() {
        val value = PresetRotationChannel("head", PresetOscillation(-0f, -0f, 1f, -0f))
        val source = """{"boneId":"head","offsetDegrees":-0.0,"amplitudeDegrees":-0.0,"phaseDegrees":-0.0}"""
        assertEquals(source, Json.encodeToString(value))
        assertEquals(value, Json.decodeFromString<PresetRotationChannel>(source))
    }
}
