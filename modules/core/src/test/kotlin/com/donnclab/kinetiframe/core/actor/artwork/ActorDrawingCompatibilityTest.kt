// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class ActorDrawingCompatibilityTest {
    private val skin = """{"sourceWidth":32,"sourceHeight":16,"columns":1,"rows":1,"bones":[],"vertices":[]}"""
    private val minimal =
        """{"inkAssetId":"ink","colourAssetId":"colour","width":32,"height":16,"originX":2.0,"originY":3.0,""" +
            """"actorWidth":64.0,"actorHeight":32.0,"skin":$skin}"""
    private val defaults = Json { encodeDefaults = true }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }

    @Test fun `legacy fields map to cohesive source placement and binding`() {
        val value = Json.decodeFromString<ActorDrawing>(minimal)
        assertEquals(DrawingSource("ink", "colour", SkinSourceSize(32, 16)), value.source)
        assertEquals(DrawingPlacement(Point2(2f, 3f), DrawingExtent(64f, 32f)), value.placement)
        assertEquals(minimal, Json.encodeToString(value))
        assertEquals(
            "com.kinetiframe.engine.rigging.model.ActorDrawing",
            ActorDrawing.serializer().descriptor.serialName,
        )
        assertEquals("@drawing-ink", ActorDrawing.INK_ADDRESS)
        assertEquals("@drawing-colour", ActorDrawing.COLOUR_ADDRESS)
    }

    @Test fun `default composition is omitted or included according to caller policy`() {
        val value = Json.decodeFromString<ActorDrawing>(minimal)
        assertEquals(DrawingComposition(), value.composition)
        val encoded = defaults.encodeToString(value)
        val expected = Json.parseToJsonElement(minimal).jsonObject + mapOf(
            "skin" to Json.parseToJsonElement(skin.dropLast(1) + ",\"attachmentBlend\":false}"),
            "enabled" to JsonPrimitive(true),
            "regions" to JsonArray(emptyList()),
            "overlayParts" to JsonArray(emptyList()),
            "underlayParts" to JsonArray(emptyList()),
        )
        assertEquals(JsonObject(expected), Json.parseToJsonElement(encoded))
        assertEquals(value, Json.decodeFromString<ActorDrawing>(encoded))
    }

    @Test fun `corrections and authored overlay sides roundtrip without group keys`() {
        val full =
            minimal.dropLast(1) +
                """, "enabled":false,"regions":[{"boneId":"face","outline":[{"x":0.1,"y":0.2}]}],"overlayParts":["hand"],"underlayParts":["leg"]}"""
        val value = Json.decodeFromString<ActorDrawing>(full)
        assertFalse(value.enabled)
        assertEquals(setOf("hand"), value.overlayParts)
        assertEquals(setOf("leg"), value.underlayParts)
        assertEquals(12f, value.regions.single().featherPixels)
        assertEquals(Json.parseToJsonElement(full), Json.parseToJsonElement(Json.encodeToString(value)))
    }

    @Test fun `every required field stays required and rejects null`() {
        val fields = Json.parseToJsonElement(minimal).jsonObject
        fields.keys.forEach { field ->
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<ActorDrawing>(JsonObject(fields - field).toString())
            }
            assertThrows(SerializationException::class.java) {
                Json.decodeFromString<ActorDrawing>(JsonObject(fields + (field to JsonNull)).toString())
            }
        }
    }

    @Test fun `unknown fields and raw invalid dimensions retain existing decode policy`() {
        val future = minimal.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ActorDrawing>(future) }
        assertEquals(
            Json.decodeFromString<ActorDrawing>(minimal),
            forwardCompatible.decodeFromString<ActorDrawing>(future),
        )
        val invalid = minimal.replace("\"width\":32", "\"width\":0")
        assertEquals(0, Json.decodeFromString<ActorDrawing>(invalid).width)
    }

    @Test fun `visibility and skin edits retain source placement corrections and composition`() {
        val initial = Json.decodeFromString<ActorDrawing>(minimal).copy(
            binding = DrawingBinding(
                Json.decodeFromString<ArtworkSkin>(skin),
                listOf(ActorDrawingRegion("face", emptyList())),
            ),
            composition = DrawingComposition(overlayParts = setOf("hand")),
        )
        val changed = initial.withEnabled(false).withSkin(initial.skin.copy(attachmentBlend = true))
        assertEquals(initial.source, changed.source)
        assertEquals(initial.placement, changed.placement)
        assertEquals(initial.regions, changed.regions)
        assertEquals(initial.overlayParts, changed.overlayParts)
        assertFalse(changed.enabled)
        assertTrue(initial.enabled)
        assertFalse(initial.skin.attachmentBlend)
    }
}
