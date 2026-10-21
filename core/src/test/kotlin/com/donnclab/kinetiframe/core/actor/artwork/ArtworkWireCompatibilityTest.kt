// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class ArtworkWireCompatibilityTest {
    private val defaults = Json { encodeDefaults = true }
    private val forwardCompatible = Json { ignoreUnknownKeys = true }
    private val matrix = """{"a":1.0,"b":0.0,"c":0.0,"d":1.0,"tx":0.0,"ty":0.0}"""
    private val mesh =
        """{"sourceWidth":8,"sourceHeight":8,"columns":1,"rows":1,"bones":[{"boneId":"arm",""" +
            """"bindWorld":$matrix}],"vertices":[{"x":0.0,"y":0.0,"weights":[1.0]}]}"""

    @Test fun `legacy flat bind matrix retains coefficients descriptor and row convention`() {
        val legacy = """{"a":2.0,"b":3.0,"c":4.0,"d":5.0,"tx":6.0,"ty":7.0}"""
        val value = Json.decodeFromString<SkinAffine>(legacy)
        assertEquals(SkinAffine(Point2(2f, 3f), Point2(4f, 5f), Point2(6f, 7f)), value)
        assertEquals(14f, value.x(1f, 2f))
        assertEquals(21f, value.y(1f, 2f))
        assertEquals(legacy, Json.encodeToString(value))
        assertEquals("com.kinetiframe.engine.rigging.model.SkinAffine", SkinAffine.serializer().descriptor.serialName)
    }

    @Test fun `grouped mesh reads old flat shape and does not persist grouped property names`() {
        val value = Json.decodeFromString<ArtworkSkin>(mesh)
        assertEquals(SkinSourceSize(8, 8), value.source)
        assertEquals(SkinGrid(1, 1), value.grid)
        assertFalse(value.attachmentBlend)
        assertEquals(Json.parseToJsonElement(mesh), Json.parseToJsonElement(Json.encodeToString(value)))
        val encoded = defaults.encodeToString(value)
        assertEquals(
            Json.parseToJsonElement(mesh).jsonObject.keys + "attachmentBlend",
            Json.parseToJsonElement(encoded).jsonObject.keys,
        )
        assertEquals("com.kinetiframe.engine.rigging.model.ArtworkSkin", ArtworkSkin.serializer().descriptor.serialName)
        assertEquals(value, Json.decodeFromString<ArtworkSkin>(encoded))
    }

    @Test fun `socket flag true survives flat roundtrip`() {
        val source = mesh.dropLast(1) + ",\"attachmentBlend\":true}"
        val value = Json.decodeFromString<ArtworkSkin>(source)
        assertTrue(value.attachmentBlend)
        assertEquals(Json.parseToJsonElement(source), Json.parseToJsonElement(Json.encodeToString(value)))
    }

    @Test fun `required fields remain required and unknown fields respect caller policy`() {
        assertThrows(SerializationException::class.java) { Json.decodeFromString<SkinAffine>("{}") }
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ArtworkSkin>("{}") }
        val withFuture = mesh.dropLast(1) + ",\"future\":true}"
        assertThrows(SerializationException::class.java) { Json.decodeFromString<ArtworkSkin>(withFuture) }
        assertEquals(
            Json.decodeFromString<ArtworkSkin>(mesh),
            forwardCompatible.decodeFromString<ArtworkSkin>(withFuture),
        )
    }

    @Test fun `inverse composition preserves scaled reflected points and rejects singular bind`() {
        val value = SkinAffine(Point2(-2f, 1f), Point2(0f, 3f), Point2(8f, -4f))
        val identity = value.times(value.inverse())
        assertEquals(5f, identity.x(5f, 6f), .00001f)
        assertEquals(6f, identity.y(5f, 6f), .00001f)
        val failure = assertThrows(SkinTransformException::class.java) {
            SkinAffine(Point2(0f, 0f), Point2(0f, 0f), Point2(0f, 0f)).inverse()
        }
        assertEquals(SkinTransformIssue.SINGULAR_BIND, failure.issue)
    }

    @Test fun `drawing corrections keep outline default and serialized identities`() {
        val value = Json.decodeFromString<ActorDrawingRegion>("""{"boneId":"arm","outline":[{"x":0.2,"y":0.3}]}""")
        assertEquals(12f, value.featherPixels)
        assertEquals(listOf(DrawingPoint(.2f, .3f)), value.outline)
        assertEquals(
            "com.kinetiframe.engine.rigging.model.ActorDrawingRegion",
            ActorDrawingRegion.serializer().descriptor.serialName,
        )
    }
}
