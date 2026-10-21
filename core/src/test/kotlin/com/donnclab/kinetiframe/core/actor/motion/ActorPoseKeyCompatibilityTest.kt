// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class ActorPoseKeyCompatibilityTest {
    private val minimal = """{"timeMicros":120,"rotations":{"arm":25.0}}"""
    private val full =
        """{"timeMicros":120,"rotations":{"arm":25.0},"rootTranslations":{"root":{"x":2.0,"y":3.0}},""" +
            """"contacts":{"leg":{"x":5.0,"y":8.0,"bendSide":-1,"followBoneId":"foot","followWorldAngle":30.0}}}"""

    @Test fun `full legacy pose key roundtrips byte equivalent with original serial identity`() {
        val value = Json.decodeFromString<ActorPoseKey>(full)
        assertEquals(120L, value.timeMicros)
        assertEquals(RootTranslation(2f, 3f), value.rootTranslations.getValue("root"))
        assertEquals(30f, value.contacts.getValue("leg").followWorldAngle)
        assertEquals(full, Json.encodeToString(value))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.ActorPoseKey",
            ActorPoseKey.serializer().descriptor.serialName,
        )
    }

    @Test fun `minimal keys omit default channels and explicit defaults remain readable`() {
        val value = Json.decodeFromString<ActorPoseKey>(minimal)
        assertEquals(ActorPoseKey(120L, mapOf("arm" to 25f)), value)
        assertEquals(minimal, Json.encodeToString(value))
        val defaults = Json { encodeDefaults = true }.encodeToString(value)
        assertEquals(minimal.dropLast(1) + ",\"rootTranslations\":{},\"contacts\":{}}", defaults)
        assertEquals(value, Json.decodeFromString<ActorPoseKey>(defaults))
    }

    @Test fun `required fields and unknown field policy remain unchanged`() {
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<ActorPoseKey>("""{"timeMicros":120}""")
        }
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ActorPoseKey>("""{"rotations":{}}""") }
        val future = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ActorPoseKey>(future) }
        assertEquals(
            Json.decodeFromString<ActorPoseKey>(minimal),
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString<ActorPoseKey>(future),
        )
    }
}
