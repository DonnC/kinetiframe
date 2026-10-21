// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class ActorArtworkKeyWireTest {
    private val defaults = Json { encodeDefaults = true }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }

    @Test fun `legacy field order identity and reset values survive roundtrip`() {
        val source = """{"timeMicros":100,"selections":{"eyes":"blink","mouth":""}}"""
        val key = Json.decodeFromString<ActorArtworkKey>(source)
        assertEquals(ActorArtworkKey(100, linkedMapOf("eyes" to "blink", "mouth" to "")), key)
        assertEquals(source, Json.encodeToString(key))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.ActorArtworkKey",
            ActorArtworkKey.serializer().descriptor.serialName,
        )
        assertEquals(source, defaults.encodeToString(key))
    }

    @Test fun `fields stay required and unknown field policy belongs to caller`() {
        listOf("{}", """{"timeMicros":0}""", """{"selections":{}}""").forEach { source ->
            assertThrows(SerializationException::class.java) { Json.decodeFromString<ActorArtworkKey>(source) }
        }
        val source = """{"timeMicros":-1,"selections":{},"future":true}"""
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ActorArtworkKey>(source) }
        assertEquals(ActorArtworkKey(-1, emptyMap()), forwardCompatible.decodeFromString<ActorArtworkKey>(source))
    }
}
