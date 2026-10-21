// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.DrawingBinding
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingSource
import com.donnclab.kinetiframe.core.actor.artwork.SceneArtwork
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class SceneGraphCompatibilityTest {
    private val legacy =
        """{"rootBoneIds":["root"],"bones":{"root":{"id":"root","name":"Root","length":10.0},""" +
            """"child":{"id":"child","parentId":"root","name":"Child","length":5.0}}}"""

    @Test fun `legacy graph remains byte equivalent with defaults omitted`() {
        val graph = Json.decodeFromString<SceneGraph>(legacy)
        assertEquals(legacy, Json.encodeToString(graph))
        assertNull(graph.drawing)
        assertEquals("root", graph.bones.getValue("child").parentId)
        assertEquals("com.kinetiframe.engine.rigging.model.SceneGraph", SceneGraph.serializer().descriptor.serialName)
    }

    @Test fun `empty and explicit defaults retain original three fields`() {
        assertEquals(SceneGraph(), Json.decodeFromString<SceneGraph>("{}"))
        assertEquals("{}", Json.encodeToString(SceneGraph()))
        val defaults = Json { encodeDefaults = true }
        val encoded = defaults.encodeToString(SceneGraph())
        assertEquals(setOf("rootBoneIds", "bones", "drawing"), Json.parseToJsonElement(encoded).jsonObject.keys)
        assertEquals(SceneGraph(), Json.decodeFromString<SceneGraph>(encoded))
    }

    @Test fun `unknown fields follow caller policy without validating at decode time`() {
        assertThrows(SerializationException::class.java) { Json.decodeFromString<SceneGraph>("""{"future":1}""") }
        assertEquals(SceneGraph(), Json { ignoreUnknownKeys = true }.decodeFromString<SceneGraph>("""{"future":1}"""))
        assertEquals(
            -10f,
            Json.decodeFromString<SceneGraph>(legacy.replace("10.0", "-10.0")).bones.getValue("root").length,
        )
    }

    @Test fun `drawing and variant addresses retain independent registrations`() {
        val base = Json.decodeFromString<SceneGraph>(legacy)
        val root = base.bones.getValue("root").withArtwork(
            textureBinding = TextureBinding("default", 2f, 3f),
            artworkVariants = mapOf("open" to ArtworkVariant("Open", TextureBinding("open", 4f, 5f))),
        )
        val skin = ArtworkSkin(
            SkinSourceSize(10, 10),
            SkinGrid(1, 1),
            SkinMesh(listOf(SkinBone("root", SkinAffine.Identity)), List(4) { SkinVertex(0f, 0f, listOf(1f)) }),
        )
        val drawing = ActorDrawing(
            DrawingSource("ink", "colour", skin.source),
            DrawingPlacement(Point2(0f, 0f), DrawingExtent(10f, 10f)),
            DrawingBinding(skin),
        )
        val graph = base.copy(bones = base.bones + ("root" to root), drawing = drawing)
        val copy = Json.decodeFromString<SceneGraph>(Json.encodeToString(graph))
        assertEquals(graph, copy)
        val bindings = SceneArtwork.bindings(copy)
        assertEquals(setOf("root", "root#open", ActorDrawing.INK_ADDRESS, ActorDrawing.COLOUR_ADDRESS), bindings.keys)
        assertEquals(4f, bindings.getValue("root#open").pivotX)
        assertEquals("ink", bindings.getValue(ActorDrawing.INK_ADDRESS).assetId)
    }
}
