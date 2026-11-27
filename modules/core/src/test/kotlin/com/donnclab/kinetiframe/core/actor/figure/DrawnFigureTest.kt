// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.figure

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrawnFigureTest {
    private val json = Json { ignoreUnknownKeys = false }

    private fun bone(id: String, parent: String?, length: Float, x: Float = 0f) = BoneNode(
        hierarchy = BoneHierarchy(id, id, parent),
        rest = BoneRest(length, localTransform = Transform(x = x)),
    )

    private val graph = SceneGraph(
        listOf("torso"),
        mapOf(
            "torso" to bone("torso", null, 100f),
            "upper" to bone("upper", "torso", 50f),
            "lower" to bone("lower", "upper", 50f, 50f),
            "hand" to bone("hand", "lower", 20f, 50f),
        ),
    )

    private val figure = DrawnFigure(
        palette = mapOf("ink" to "#0E0A0A", "skin" to "#F8BF91", "shirt" to "#12B1C2"),
        tubes = listOf(FigureTube("arm", listOf("upper", "lower"), 56f, "shirt", cuff = true, rootPartId = "body")),
        parts = listOf(
            FigurePart(
                "body",
                "torso",
                mapOf("default" to listOf(FigureShape("M 0 -40 L 100 -40 L 100 40 L 0 40 Z", "shirt"))),
            ),
            FigurePart(
                "hand",
                "hand",
                mapOf(
                    "default" to listOf(FigureShape("M -6 22 C 12 30 40 32 56 22 C 70 12 72 -12 58 -20 Z", "skin")),
                    "fist" to listOf(FigureShape("M -6 22 C 8 33 38 35 50 20 Z", "skin")),
                ),
            ),
        ),
    )

    @Test fun `parses the SVG subset with repeated arguments`() {
        val commands = FigurePath.parse("M 0 0 L 10 0 20 5 Q 1 2 3 4 C 1,2 3,4 5,6 Z")
        assertEquals(PathCommand.MoveTo(0f, 0f), commands[0])
        assertEquals(PathCommand.LineTo(10f, 0f), commands[1])
        assertEquals(PathCommand.LineTo(20f, 5f), commands[2])
        assertEquals(PathCommand.QuadTo(1f, 2f, 3f, 4f), commands[3])
        assertEquals(PathCommand.CubicTo(1f, 2f, 3f, 4f, 5f, 6f), commands[4])
        assertEquals(PathCommand.Close, commands[5])
        // Numbers after M continue as lines, as in SVG.
        assertEquals(PathCommand.LineTo(3f, -4.5f), FigurePath.parse("M 1 2 3 -4.5")[1])
    }

    @Test fun `rejects paths it cannot draw`() {
        assertThrows(IllegalArgumentException::class.java) { FigurePath.parse("L 1 2") }
        assertThrows(IllegalArgumentException::class.java) { FigurePath.parse("M 1") }
        assertThrows(IllegalArgumentException::class.java) { FigurePath.parse("M 1 2 A 3 4 5 6 7 8 9") }
        assertThrows(IllegalArgumentException::class.java) { FigurePath.parse("M 0 0 " + "L 1 1 ".repeat(800)) }
    }

    @Test fun `validates bones colours and limb structure`() {
        FigureValidation.validate(figure, graph)
        fun invalid(change: DrawnFigure) =
            assertThrows(IllegalArgumentException::class.java) { FigureValidation.validate(change, graph) }
        invalid(figure.copy(ink = "missing"))
        invalid(figure.copy(palette = figure.palette + ("bad" to "teal")))
        invalid(figure.copy(tubes = listOf(figure.tubes[0].copy(boneIds = listOf("torso", "lower")))))
        invalid(figure.copy(tubes = listOf(figure.tubes[0].copy(fill = "nope"))))
        invalid(figure.copy(tubes = listOf(figure.tubes[0].copy(boneIds = emptyList()))))
        invalid(figure.copy(tubes = listOf(figure.tubes[0].copy(endWidth = 0f))))
        invalid(figure.copy(tubes = listOf(figure.tubes[0].copy(rootPartId = "ghost"))))
        invalid(figure.copy(parts = figure.parts + figure.parts[1].copy(id = "extra", boneId = "tail")))
        invalid(figure.copy(parts = listOf(figure.parts[0], figure.parts[0])))
    }

    @Test fun `any thing validates the same way, such as a tapering tail and a flag`() {
        // A three-bone tail on the torso and a cloth that follows two bones: nothing human.
        val tail = graph.copy(
            bones = graph.bones +
                ("t1" to bone("t1", "torso", 30f)) + ("t2" to bone("t2", "t1", 30f, 30f)) +
                ("t3" to bone("t3", "t2", 30f, 30f)),
        )
        val bind = SkinAffine(Point2(1f, 0f), Point2(0f, 1f), Point2(0f, 0f))
        val thing = figure.copy(
            tubes = listOf(FigureTube("tail", listOf("t1", "t2", "t3"), 20f, "shirt", endWidth = 4f)),
            parts = emptyList(),
            surfaces = listOf(
                FigureSurface(
                    "flag",
                    listOf(SurfaceBone("t1", bind, 30f), SurfaceBone("t2", bind, 30f)),
                    listOf(FigureShape("M 0 0 L 60 0 L 60 30 L 0 30 Z", "skin")),
                ),
            ),
        )
        FigureValidation.validate(thing, tail)
        assertThrows(IllegalArgumentException::class.java) {
            FigureValidation.validate(
                thing.copy(tubes = listOf(thing.tubes[0].copy(boneIds = listOf("t1", "t3")))),
                tail,
            )
        }
        val singular = SkinAffine(Point2(0f, 0f), Point2(0f, 0f), Point2(0f, 0f))
        assertThrows(IllegalArgumentException::class.java) {
            FigureValidation.validate(
                thing.copy(surfaces = listOf(thing.surfaces[0].copy(bones = listOf(SurfaceBone("t1", singular, 30f))))),
                tail,
            )
        }
    }

    @Test fun `artwork selections pick only variants the figure has`() {
        val selected = figure.select(mapOf("hand" to "fist", "torso" to "missing", "lower" to "fist"))
        assertEquals(mapOf("hand" to "fist"), selected.selections)
        assertEquals(
            "M -6 22 C 8 33 38 35 50 20 Z",
            selected.parts[1].shapes(selected.selections["hand"]).single().path,
        )
        assertEquals(figure.parts[1].variants.getValue("default"), figure.parts[1].shapes("unknown"))
    }

    @Test fun `a scene graph without a figure saves exactly as before`() {
        val plain = SceneGraph(listOf("torso"), mapOf("torso" to bone("torso", null, 100f)))
        val encoded = json.encodeToString(plain)
        assertFalse(encoded.contains("figure"))
        val old = """{"rootBoneIds":["torso"],"bones":{"torso":{"id":"torso","name":"torso","length":100.0}}}"""
        assertNull(json.decodeFromString<SceneGraph>(old).figure)
    }

    @Test fun `a figure survives a save and reload`() {
        val withFigure = graph.copy(figure = figure)
        val decoded = json.decodeFromString<SceneGraph>(json.encodeToString(withFigure))
        assertEquals(figure, decoded.figure)
        assertTrue(json.encodeToString(withFigure).contains("\"tubes\""))
    }
}
