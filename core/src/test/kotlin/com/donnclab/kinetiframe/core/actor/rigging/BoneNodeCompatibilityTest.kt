// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class BoneNodeCompatibilityTest {
    private val minimal = """{"id":"hand","name":"Hand","length":12.0}"""
    private val full =
        """{"id":"hand","parentId":"arm","name":"Hand","length":12.0,"localTransform":{"x":4.0},""" +
            """"textureSlotId":"slot","textureBinding":{"assetId":"open","pivotX":2.0,"pivotY":3.0},""" +
            """"ikChainRootId":"shoulder","visible":false,"drawOrder":7,""" +
            """"artworkVariants":{"closed":{"name":"Closed","binding":{"assetId":"closed","pivotX":2.0,""" +
            """"pivotY":3.0}}}}"""
    private val defaults = Json { encodeDefaults = true }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }

    @Test fun `flat legacy fields map to the four cohesive records`() {
        val value = Json.decodeFromString<BoneNode>(full)
        assertEquals(BoneHierarchy("hand", "Hand", "arm"), value.hierarchy)
        assertEquals(BoneRest(12f, Transform(4f), "shoulder"), value.rest)
        assertEquals(BoneDisplay(false, 7), value.display)
        assertEquals("open", value.textureBinding?.assetId)
        assertEquals("closed", value.artworkVariants.getValue("closed").binding.assetId)
        assertEquals(full, Json.encodeToString(value))
        assertEquals("com.kinetiframe.engine.rigging.model.BoneNode", BoneNode.serializer().descriptor.serialName)
        assertEquals(
            "com.kinetiframe.engine.rigging.model.ArtworkVariant",
            ArtworkVariant.serializer().descriptor.serialName,
        )
    }

    @Test fun `minimal archives keep defaults and explicit default output remains flat`() {
        val value = Json.decodeFromString<BoneNode>(minimal)
        assertEquals(BoneNode("hand", name = "Hand", length = 12f), value)
        assertEquals(minimal, Json.encodeToString(value))
        val encoded = defaults.encodeToString(value)
        assertEquals(
            setOf(
                "id", "parentId", "name", "length", "localTransform", "textureSlotId", "textureBinding",
                "ikChainRootId", "visible", "drawOrder", "artworkVariants",
            ),
            Json.parseToJsonElement(encoded).jsonObject.keys,
        )
        assertEquals(value, Json.decodeFromString<BoneNode>(encoded))
    }

    @Test fun `required fields stay required and nullable defaults remain nullable`() {
        val fields = Json.parseToJsonElement(minimal).jsonObject
        fields.keys.forEach { field ->
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<BoneNode>(JsonObject(fields - field).toString())
            }
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<BoneNode>(JsonObject(fields + (field to JsonNull)).toString())
            }
        }
        val value = Json.decodeFromString<BoneNode>(minimal.dropLast(1) + ",\"parentId\":null,\"textureBinding\":null}")
        assertEquals(Json.decodeFromString<BoneNode>(minimal), value)
    }

    @Test fun `unknown fields retain caller policy and invalid raw values await graph validation`() {
        val future = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<BoneNode>(future) }
        assertEquals(Json.decodeFromString<BoneNode>(minimal), forwardCompatible.decodeFromString<BoneNode>(future))
        assertEquals(-12f, Json.decodeFromString<BoneNode>(minimal.replace("12.0", "-12.0")).length)
    }

    @Test fun `each edit method preserves unrelated records and the original bone`() {
        val value = Json.decodeFromString<BoneNode>(full)
        val posed = value.withRest(localTransform = Transform(20f, 30f))
        assertEquals(value.hierarchy, posed.hierarchy)
        assertEquals(value.artwork, posed.artwork)
        assertEquals(value.display, posed.display)
        assertEquals(value.ikChainRootId, posed.ikChainRootId)
        assertEquals(4f, value.localTransform.x)
        val renamed = value.withHierarchy(name = "Palm")
        assertEquals(value.rest, renamed.rest)
        assertEquals(value.artwork, renamed.artwork)
        val hidden = value.withDisplay(visible = true)
        assertTrue(hidden.visible)
        assertFalse(value.visible)
        assertEquals(7, hidden.drawOrder)
        val cleared = value.withArtwork(textureSlotId = null, textureBinding = null)
        assertEquals(value.artworkVariants, cleared.artworkVariants)
        assertEquals(value.rest, cleared.rest)
        assertEquals(value.display, cleared.display)
    }

    @Test fun `variant binding retains independent registration across roundtrip`() {
        val value = BoneNode("hand", name = "Hand", length = 12f).withArtwork(
            textureBinding = TextureBinding("neutral", 1f, 2f),
            artworkVariants = mapOf("open" to ArtworkVariant("Open", TextureBinding("open", 3f, 4f))),
        )
        assertEquals(value, Json.decodeFromString<BoneNode>(defaults.encodeToString(value)))
        assertEquals(1f, value.textureBinding?.pivotX)
        assertEquals(3f, value.artworkVariants.getValue("open").binding.pivotX)
    }
}
