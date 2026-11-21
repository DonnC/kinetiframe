// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class TextureBindingCompatibilityTest {
    private val minimal = """{"assetId":"arm","pivotX":2.0,"pivotY":3.0}"""
    private val full =
        """{"assetId":"arm","pivotX":2.0,"pivotY":3.0,"offsetX":4.0,"offsetY":5.0,"rotationDegrees":90.0,""" +
            """"scale":2.0,"skin":null}"""
    private val defaults = Json { encodeDefaults = true }
    private val omitNulls = Json {
        encodeDefaults = true
        explicitNulls = false
    }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }

    @Test fun `legacy flat alignment reads grouped geometry without a schema change`() {
        val binding = Json.decodeFromString<TextureBinding>(full)
        assertEquals(Point2(2f, 3f), binding.pivot)
        assertEquals(ArtworkAlignment(Point2(4f, 5f), 90f, 2f), binding.alignment)
        assertEquals(full, defaults.encodeToString(binding))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.TextureBinding",
            TextureBinding.serializer().descriptor.serialName,
        )
    }

    @Test fun `old minimal fields retain defaults and default encoding policy`() {
        val binding = Json.decodeFromString<TextureBinding>(minimal)
        assertEquals(TextureBinding("arm", 2f, 3f), binding)
        assertEquals(minimal, Json.encodeToString(binding))
        val encoded = defaults.encodeToString(binding)
        assertEquals(
            """{"assetId":"arm","pivotX":2.0,"pivotY":3.0,"offsetX":0.0,"offsetY":0.0,"rotationDegrees":0.0,"scale":1.0,"skin":null}""",
            encoded,
        )
        assertFalse(omitNulls.encodeToString(binding).contains("skin"))
    }

    @Test fun `each required field remains required and null cannot replace it`() {
        listOf("assetId", "pivotX", "pivotY").forEach { field ->
            val objectFields = kotlinx.serialization.json.Json.parseToJsonElement(
                minimal,
            ) as kotlinx.serialization.json.JsonObject
            val without = kotlinx.serialization.json.JsonObject(objectFields - field).toString()
            val failure =
                assertThrows(MissingFieldException::class.java) { Json.decodeFromString<TextureBinding>(without) }
            assertTrue(field in failure.missingFields)
            val nullField = kotlinx.serialization.json.JsonObject(
                objectFields + (field to kotlinx.serialization.json.JsonNull),
            ).toString()
            assertThrows(SerializationException::class.java) { Json.decodeFromString<TextureBinding>(nullField) }
        }
    }

    @Test fun `unknown fields respect the archive reader policy`() {
        val future = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<TextureBinding>(future) }
        assertEquals(
            Json.decodeFromString<TextureBinding>(minimal),
            forwardCompatible.decodeFromString<TextureBinding>(future),
        )
    }

    @Test fun `nested continuous skin keeps flat mesh and bind fields`() {
        val skin = ArtworkSkin(
            SkinSourceSize(10, 20),
            SkinGrid(1, 1),
            SkinMesh(listOf(SkinBone("arm", SkinAffine.Identity)), listOf(SkinVertex(2f, 3f, listOf(1f)))),
        )
        val binding = TextureBinding("arm", 2f, 3f).copy(skin = skin)
        val encoded = defaults.encodeToString(binding)
        assertTrue(encoded.contains("\"sourceWidth\":10"))
        assertTrue(encoded.contains("\"bindWorld\":{\"a\":1.0"))
        assertFalse(encoded.contains("alignment"))
        assertEquals(binding, Json.decodeFromString<TextureBinding>(encoded))
    }

    @Test fun `decoding preserves invalid raw alignment for graph validation`() {
        val invalid = minimal.dropLast(1) + ",\"scale\":0.0}"
        assertEquals(0f, Json.decodeFromString<TextureBinding>(invalid).scale)
    }
}
