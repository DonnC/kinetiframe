package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.figure.DrawnFigure
import com.donnclab.kinetiframe.core.actor.figure.FigurePart
import com.donnclab.kinetiframe.core.actor.figure.FigureShape
import com.donnclab.kinetiframe.core.actor.figure.FigureSurface
import com.donnclab.kinetiframe.core.actor.figure.FigureTube
import com.donnclab.kinetiframe.core.actor.figure.SurfaceBone
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FigureRenderTest {
    private fun bone(id: String, parent: String?, length: Float, x: Float = 0f, y: Float = 0f, angle: Float = 0f) =
        BoneNode(
            hierarchy = BoneHierarchy(id, id, parent),
            rest = BoneRest(length, localTransform = Transform(x = x, y = y, rotationDegrees = angle)),
        )

    // A torso pointing down from (100, 60), an arm bent at the elbow, and a hand at the wrist.
    private val graph = SceneGraph(
        listOf("torso"),
        mapOf(
            "torso" to bone("torso", null, 120f, x = 100f, y = 60f, angle = 90f),
            "upper" to bone("upper", "torso", 70f, x = 20f, angle = -60f),
            "lower" to bone("lower", "upper", 70f, x = 70f, angle = -60f),
            "hand" to bone("hand", "lower", 20f, x = 70f),
        ),
        figure = DrawnFigure(
            palette = mapOf("ink" to "#000000", "skin" to "#F8BF91", "shirt" to "#12B1C2"),
            tubes = listOf(
                FigureTube(
                    "arm",
                    listOf("upper", "lower"),
                    40f,
                    "shirt",
                    drawOrder = 1,
                    cuff = true,
                    rootPartId = "body",
                ),
            ),
            parts = listOf(
                FigurePart(
                    "body",
                    "torso",
                    mapOf("default" to listOf(FigureShape("M 0 -50 L 120 -50 L 120 50 L 0 50 Z", "shirt"))),
                ),
                FigurePart(
                    "hand",
                    "hand",
                    mapOf(
                        "default" to listOf(FigureShape("M -6 18 C 10 26 34 26 44 16 C 54 6 54 -12 44 -16 Z", "skin")),
                    ),
                    drawOrder = 9,
                ),
            ),
        ),
    )

    private fun render(): Bitmap {
        val commands = PuppetRenderPlan.create(PuppetDefinition("f", "Figure", graph), PuppetInstance("i", "f"))
        assertEquals(PuppetRenderCommand.FIGURE_ASSET, commands.single().assetId)
        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        PuppetArtworkRenderer.draw(Canvas(bitmap), commands.single(), Paint()) { null }
        return bitmap
    }

    @Test fun `draws the body, a bent sleeve and the hand over the cuff`() {
        val world = com.donnclab.kinetiframe.actors.rig.math.ForwardKinematicsSolver.resolveWorldStates(graph)
        val elbow = world.getValue("lower").startPoint
        val wrist = world.getValue("lower").endPoint
        val bitmap = render()
        // The sleeve is drawn through the elbow, so the joint is filled with the shirt colour.
        val atElbow = bitmap.getPixel(elbow.x.toInt(), elbow.y.toInt())
        assertEquals(Color.parseColor("#12B1C2"), atElbow)
        // The hand reaches past the wrist along the forearm.
        val lower = world.getValue("lower")
        val reach = wrist + (lower.endPoint - lower.startPoint) / 70f * 30f
        assertEquals(Color.parseColor("#F8BF91"), bitmap.getPixel(reach.x.toInt(), reach.y.toInt()))
    }

    @Test fun `the hand is drawn with its tube, before the cuff`() {
        val drawing = PuppetRenderPlan.create(PuppetDefinition("f", "Figure", graph), PuppetInstance("i", "f"))
            .single().figure
        assertNotNull(drawing)
        val skin = Color.parseColor("#F8BF91")
        val shirt = Color.parseColor("#12B1C2")
        val fills = drawing!!.strokes.mapNotNull { it.fill }
        // Body, sleeve, hand, then the cuff band over the wrist.
        assertEquals(listOf(shirt, shirt, skin, shirt), fills)
        assertTrue(drawing.strokes[1].hiddenOutline != null)
    }

    @Test fun `bounds cover the whole figure for selection`() {
        val command = PuppetRenderPlan.create(PuppetDefinition("f", "Figure", graph), PuppetInstance("i", "f")).single()
        val corners = requireNotNull(PuppetArtworkRenderer.outline(command) { null })
        assertTrue(corners[0] < 100f && corners[4] > 160f)
    }

    @Test fun `a flag's cloth follows the bones it is bound to`() {
        // A pole with two bones; the cloth is drawn in the rest pose and bound to both.
        val rest = SceneGraph(
            listOf("pole"),
            mapOf(
                "pole" to bone("pole", null, 100f, x = 50f, y = 150f),
                "tip" to bone("tip", "pole", 100f, x = 100f),
            ),
        )
        val bindPole = SkinAffine(Point2(1f, 0f), Point2(0f, 1f), Point2(50f, 150f))
        val bindTip = SkinAffine(Point2(1f, 0f), Point2(0f, 1f), Point2(150f, 150f))
        val figure = DrawnFigure(
            palette = mapOf("ink" to "#000000", "cloth" to "#D23C3C"),
            surfaces = listOf(
                FigureSurface(
                    "flag",
                    listOf(SurfaceBone("pole", bindPole, 100f), SurfaceBone("tip", bindTip, 100f)),
                    listOf(FigureShape("M 50 150 L 250 150 L 250 190 L 50 190 Z", "cloth")),
                ),
            ),
        )
        // Bend the tip bone up by 45 degrees: the far end of the cloth must rise with it.
        val posed = rest.copy(
            bones = rest.bones + ("tip" to bone("tip", "pole", 100f, x = 100f, angle = -45f)),
            figure = figure,
        )
        val command = PuppetRenderPlan.create(PuppetDefinition("f", "Flag", posed), PuppetInstance("i", "f")).single()
        val bounds = requireNotNull(command.figure).bounds
        assertTrue("cloth rose with the tip: ${bounds.top}", bounds.top < 100f)
        // The cloth near the pole's root stays where it was drawn.
        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        PuppetArtworkRenderer.draw(Canvas(bitmap), command, Paint()) { null }
        assertEquals(Color.parseColor("#D23C3C"), bitmap.getPixel(60, 170))
    }
}
