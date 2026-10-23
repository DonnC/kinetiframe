package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinBuilder
import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContinuousArtworkRenderTest {
    @Test fun `attachment blend retains both parent and child surfaces`() {
        val body = BoneNode(
            hierarchy = BoneHierarchy("body", "Body", null),
            rest = BoneRest(100f),
            artwork = BoneArtwork(textureBinding = TextureBinding("body-art", 0f, 0f)),
        )
        val binding = TextureBinding("head-art", 20f, 10f)
        val head = BoneNode(
            hierarchy = BoneHierarchy("head", "Head", "body"),
            rest = BoneRest(100f, localTransform = Transform(x = 100f)),
            artwork = BoneArtwork(textureBinding = binding),
        )
        val graph = SceneGraph(listOf("body"), mapOf("body" to body, "head" to head))
        val skin = com.donnclab.kinetiframe.actors.rig.math.ArtworkAttachmentBlend.bind(
            graph,
            "head",
            binding,
            120,
            20,
        )
        val attached = graph.copy(
            bones =
            graph.bones + ("head" to head.withArtwork(textureBinding = binding.copy(skin = skin))),
        )
        ActorGraphValidation.validate(attached)
        val commands = PuppetRenderPlan.create(PuppetDefinition("rig", "Rig", attached), PuppetInstance("actor", "rig"))
        assertEquals(setOf("body-art", "head-art"), commands.map { it.assetId }.toSet())
        assertNull(commands.single { it.assetId == "body-art" }.mesh)
        assertNotNull(commands.single { it.assetId == "head-art" }.mesh)
    }

    @Test fun `continuous source renders a bend without a painted internal cut and covers old child`() {
        val binding = TextureBinding("continuous", 0f, 20f)
        val upper = BoneNode(
            hierarchy = BoneHierarchy("upper", "Upper", null),
            rest = BoneRest(100f, localTransform = Transform(40f, 60f)),
        )
        val lower = BoneNode(
            hierarchy = BoneHierarchy("lower", "Lower", "upper"),
            rest = BoneRest(100f, localTransform = Transform(x = 100f)),
            artwork = BoneArtwork(textureBinding = TextureBinding("old-cutout", 0f, 0f)),
        )
        val graph = SceneGraph(listOf("upper"), mapOf("upper" to upper, "lower" to lower))
        val skin = ArtworkSkinBuilder.bind(graph, "upper", "lower", binding, 200, 40)
        val source = Bitmap.createBitmap(200, 40, Bitmap.Config.ARGB_8888)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG)
        val sourceCanvas = Canvas(source)
        ink.color = Color.BLACK
        sourceCanvas.drawRoundRect(0f, 0f, 200f, 40f, 18f, 18f, ink)
        ink.color = Color.CYAN
        sourceCanvas.drawRoundRect(3f, 3f, 197f, 37f, 15f, 15f, ink)
        try {
            for (angle in listOf(0f, 45f, 90f, 120f)) {
                val posed = graph.copy(
                    bones = mapOf(
                        "upper" to upper.withArtwork(textureBinding = binding.copy(skin = skin)),
                        "lower" to lower.withRest(localTransform = Transform(x = 100f, rotationDegrees = angle)),
                    ),
                )
                val commands = PuppetRenderPlan.create(
                    PuppetDefinition("rig", "Rig", posed),
                    PuppetInstance("actor", "rig"),
                )
                assertEquals(listOf("continuous"), commands.map { it.assetId })
                val output = Bitmap.createBitmap(280, 220, Bitmap.Config.ARGB_8888)
                try {
                    PuppetArtworkRenderer.draw(Canvas(output), commands.single(), source, ink)
                    assertEquals("joint interior at $angle", Color.CYAN, output.getPixel(140, 60))
                    val proof = File("../.artifacts/rigging/continuous-limb-${angle.toInt()}.png")
                    proof.parentFile!!.mkdirs()
                    val review = Bitmap.createBitmap(280, 220, Bitmap.Config.ARGB_8888)
                    try {
                        review.eraseColor(Color.WHITE)
                        Canvas(review).drawBitmap(output, 0f, 0f, null)
                        proof.outputStream().use { review.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally {
                        review.recycle()
                    }
                    val mirrored = commands.single().copy(
                        matrix = Matrix().apply {
                            setScale(-1f, 1f)
                            postTranslate(280f, 0f)
                        },
                    )
                    val extent = PuppetArtworkRenderer.outline(mirrored, 200, 40)
                    assertTrue(extent.all(Float::isFinite))
                    assertTrue(extent.filterIndexed { i, _ -> i % 2 == 1 }.max() >= 60f)
                    output.eraseColor(Color.TRANSPARENT)
                    PuppetArtworkRenderer.draw(Canvas(output), mirrored, source, ink)
                    assertEquals(Color.CYAN, output.getPixel(139, 60))
                } finally {
                    output.recycle()
                }
            }
            val rigid = PuppetRenderPlan.create(
                PuppetDefinition(
                    "rig",
                    "Rig",
                    graph.copy(
                        bones = graph.bones +
                            ("upper" to upper.withArtwork(textureBinding = binding)),
                    ),
                ),
                PuppetInstance("actor", "rig"),
            )
            assertEquals(setOf("continuous", "old-cutout"), rigid.map { it.assetId }.toSet())
        } finally {
            source.recycle()
        }
    }
}
