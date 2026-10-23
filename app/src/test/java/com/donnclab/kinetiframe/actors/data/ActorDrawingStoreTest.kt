package com.donnclab.kinetiframe.actors.data

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.actors.render.PuppetRenderPlan
import com.donnclab.kinetiframe.actors.rig.math.ActorDrawingBinder
import com.donnclab.kinetiframe.actors.rig.model.ActorArtwork
import com.donnclab.kinetiframe.actors.rig.model.ActorGraphValidation
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.DrawingBinding
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingSource
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.core.document.PuppetInstance
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActorDrawingStoreTest {
    @Test fun `mixed artwork respects opt in visibility and variant choice`() {
        val root = BoneNode(
            hierarchy = BoneHierarchy("root", "Root", null),
            rest = BoneRest(32f),
            artwork = BoneArtwork(
                textureBinding = TextureBinding("detail", 0f, 0f),
                artworkVariants = mapOf("happy" to ArtworkVariant("Happy", TextureBinding("happy", 0f, 0f))),
            ),
        )
        val base = SceneGraph(listOf("root"), mapOf("root" to root))
        val drawing = ActorDrawing(
            DrawingSource("ink", "colour", SkinSourceSize(32, 16)),
            DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
            DrawingBinding(
                ActorDrawingBinder.bind(
                    base,
                    SkinSourceSize(32, 16),
                    DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
                ),
            ),
        )
        val instance = PuppetInstance("instance", "actor")
        fun plan(graph: SceneGraph) = PuppetRenderPlan.create(PuppetDefinition("actor", "Actor", graph), instance)
        val sketch = base.copy(drawing = drawing)
        assertEquals(listOf("colour", "ink"), plan(sketch).map { it.assetId })
        val mixed = sketch.copy(
            drawing = drawing.copy(composition = drawing.composition.copy(overlayParts = setOf("root"))),
        )
        assertEquals(listOf("colour", "ink", "detail"), plan(mixed).map { it.assetId })
        val farSide = mixed.copy(
            drawing = drawing.copy(composition = drawing.composition.copy(underlayParts = setOf("root"))),
        )
        assertEquals(listOf("detail", "colour", "ink"), plan(farSide).map { it.assetId })
        assertThrows(IllegalArgumentException::class.java) {
            ActorGraphValidation.validate(
                mixed.copy(
                    drawing = drawing.copy(
                        composition = drawing.composition.copy(
                            overlayParts = setOf("root"),
                            underlayParts = setOf("root"),
                        ),
                    ),
                ),
            )
        }
        val acted = ActorArtwork.apply(mixed, listOf(ActorArtworkKey(0L, mapOf("root" to "happy"))), 0L)
        assertEquals("happy", plan(acted).last().assetId)
        assertEquals(2, plan(mixed.copy(bones = mixed.bones + ("root" to root.withDisplay(visible = false)))).size)
    }

    @Test fun `both editable layers cold restore and render over the same mesh`() {
        val directory = Files.createTempDirectory("actor-drawing").toFile()
        val assets = RigAssetManager()
        val restored = RigAssetManager()
        try {
            val base = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 32f)))
            val drawing = ActorDrawing(
                DrawingSource("ink", "colour", SkinSourceSize(32, 16)),
                DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
                DrawingBinding(
                    ActorDrawingBinder.bind(
                        base,
                        SkinSourceSize(32, 16),
                        DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
                    ),
                ),
            )
            val graph = base.copy(drawing = drawing)
            assets.registerAsset(
                "ink",
                Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888).apply {
                    setPixel(5, 5, Color.BLACK)
                },
            )
            assets.registerAsset(
                "colour",
                Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.CYAN)
                },
            )
            val store = ActorArtworkStore(ActorFileStore(directory))
            val paths = store.save("actor", "rev", graph, assets)
            assets.clear()
            assertTrue(store.restore("actor", paths, graph, restored).isEmpty())
            assertEquals(Color.BLACK, restored.getAsset("ink")!!.getPixel(5, 5))
            assertEquals(Color.CYAN, restored.getAsset("colour")!!.getPixel(5, 5))
            val commands = PuppetRenderPlan.create(
                PuppetDefinition("actor", "Actor", graph),
                PuppetInstance("instance", "actor"),
            )
            assertEquals(listOf("colour", "ink"), commands.map { it.assetId })
            assertSame(commands[0].mesh, commands[1].mesh)
            val rendered = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888)
            try {
                val canvas = android.graphics.Canvas(rendered)
                commands.forEach { command ->
                    com.donnclab.kinetiframe.actors.render.PuppetArtworkRenderer.draw(
                        canvas,
                        command,
                        restored.getAsset(command.assetId)!!,
                        android.graphics.Paint(),
                    )
                }
                assertEquals(Color.CYAN, rendered.getPixel(16, 8))
                assertEquals(Color.BLACK, rendered.getPixel(5, 5))
            } finally {
                rendered.recycle()
            }
            assertTrue(
                PuppetRenderPlan.create(
                    PuppetDefinition("actor", "Actor", graph.copy(drawing = drawing.withEnabled(false))),
                    PuppetInstance("instance", "actor"),
                ).isEmpty(),
            )
        } finally {
            assets.clear()
            restored.clear()
            directory.deleteRecursively()
        }
    }
}
