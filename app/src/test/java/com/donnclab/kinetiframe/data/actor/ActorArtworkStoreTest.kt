package com.donnclab.kinetiframe.data.actor

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.actor.rigging.BoneArtwork
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkVariant
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import org.junit.Assert.assertEquals
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
class ActorArtworkStoreTest {
    @Test fun `continuous skin and original pixels survive library export import and cold restore`() {
        val root = Files.createTempDirectory("continuous-artwork-test").toFile()
        val runtime = RigAssetManager()
        val restored = RigAssetManager()
        try {
            val files = ActorFileStore(root)
            val upper = BoneNode("upper", name = "Upper", length = 50f)
            val lower = BoneNode(
                hierarchy = BoneHierarchy("lower", "Lower", "upper"),
                rest = BoneRest(50f, localTransform = Transform(x = 50f)),
            )
            val rest = SceneGraph(listOf("upper"), mapOf("upper" to upper, "lower" to lower))
            val binding = TextureBinding("continuous", 0f, 10f)
            val skin = com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinBuilder.bind(
                rest,
                "upper",
                "lower",
                binding,
                100,
                20,
            )
            val graph = rest.copy(
                bones =
                rest.bones + ("upper" to upper.withArtwork(textureBinding = binding.copy(skin = skin))),
            )
            runtime.registerAsset(
                "continuous",
                Bitmap.createBitmap(100, 20, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.CYAN)
                },
            )
            val store = ActorArtworkStore(files)
            val paths = store.save("actor", "revision", graph, runtime)
            val repository = ActorRepository(files)
            fun <T> value(result: ActorStoreResult<T>): T = when (result) {
                is ActorStoreResult.Value -> result.value
                is ActorStoreResult.Failure -> error(result.message)
            }
            value(
                repository.save(
                    ActorManifest(
                        actorId = "actor",
                        displayName = "Continuous limb",
                        templateId = ActorManifest.CUSTOM_TEMPLATE_ID,
                        currentRevisionId = "revision",
                        createdAtEpochMs = 1,
                        updatedAtEpochMs = 1,
                    ),
                    ActorRevision(
                        revisionId = "revision",
                        actorId = "actor",
                        sceneGraph = graph,
                        artworkAssetPaths = paths,
                        createdAtEpochMs = 1,
                    ),
                ),
            )
            runtime.clear()
            val bundles = ActorBundleRepository(files, repository, PerformanceRepository(files))
            value(bundles.exportActor("actor", "library-skin", "Continuous limb", 2))
            value(bundles.importActor("library-skin", "copy", "copy-revision", 3))
            val copy = value(ActorRepository(ActorFileStore(root)).load("copy")).second
            assertEquals(skin, copy.sceneGraph.bones.getValue("upper").textureBinding!!.skin)
            assertTrue(store.restore("copy", copy.artworkAssetPaths, copy.sceneGraph, restored).isEmpty())
            assertEquals(Color.CYAN, restored.getAsset("continuous")!!.getPixel(50, 10))
        } finally {
            runtime.clear()
            restored.clear()
            root.deleteRecursively()
        }
    }

    @Test fun `aligned texture scale preserves pivot and applies size in stage units`() {
        val graph = SceneGraph(
            listOf("head"),
            mapOf(
                "head" to BoneNode(
                    hierarchy = BoneHierarchy("head", "Head", null),
                    rest = BoneRest(10f, localTransform = Transform(100f, 200f)),
                    artwork = BoneArtwork(
                        textureBinding = TextureBinding("head-art", 10f, 20f).let {
                            it.copy(alignment = it.alignment.copy(scale = .5f))
                        },
                    ),
                ),
            ),
        )
        val definition = com.donnclab.kinetiframe.engine.model.PuppetDefinition("actor", "Actor", graph)
        val matrix = com.donnclab.kinetiframe.engine.rigging.render.PuppetRenderPlan.create(
            definition,
            com.donnclab.kinetiframe.engine.model.PuppetInstance("instance", "actor"),
        ).single().matrix
        val points = floatArrayOf(10f, 20f, 30f, 20f)
        matrix.mapPoints(points)
        assertEquals(100f, points[0], .001f)
        assertEquals(200f, points[1], .001f)
        assertEquals(110f, points[2], .001f)
    }

    @Test fun `pack padding is cropped around visible pixels before alignment`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(20, 40, Color.RED)
        bitmap.setPixel(29, 49, Color.BLUE)
        val cropped = ActorArtworkDecoder.trimTransparentPadding(bitmap)
        try {
            assertTrue(bitmap.isRecycled)
            assertEquals(10, cropped.width)
            assertEquals(10, cropped.height)
            assertEquals(Color.RED, cropped.getPixel(0, 0))
        } finally {
            cropped.recycle()
        }
    }

    @Test fun `default and variant pixels survive clearing runtime and reloading from disk`() {
        val root = Files.createTempDirectory("actor-artwork-test").toFile()
        val runtime = RigAssetManager()
        val restored = RigAssetManager()
        try {
            runtime.registerAsset(
                "default",
                Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.RED)
                },
            )
            runtime.registerAsset(
                "smile",
                Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.BLUE)
                },
            )
            val graph =
                SceneGraph(
                    listOf("mouth"),
                    mapOf(
                        "mouth" to BoneNode(
                            hierarchy = BoneHierarchy("mouth", "Mouth", null),
                            rest = BoneRest(5f),
                            artwork = BoneArtwork(
                                textureBinding = TextureBinding("default", 0f, 0f),
                                artworkVariants = mapOf(
                                    "happy" to
                                        ArtworkVariant(
                                            "Happy",
                                            TextureBinding("smile", 1f, 2f).let {
                                                it.copy(alignment = it.alignment.copy(scale = .5f))
                                            },
                                        ),
                                ),
                            ),
                        ),
                    ),
                )
            val store = ActorArtworkStore(ActorFileStore(root))
            val paths = store.save("actor", "revision", graph, runtime)
            assertEquals(setOf("mouth", "mouth#happy"), paths.keys)
            runtime.clear()
            assertTrue(store.restore("actor", paths, graph, restored).isEmpty())
            assertEquals(Color.RED, restored.getAsset("default")!!.getPixel(0, 0))
            assertEquals(Color.BLUE, restored.getAsset("smile")!!.getPixel(0, 0))
            assertTrue(restored.allocatedBytes() > 0)
            val files = ActorFileStore(root)
            val repository = ActorRepository(files)
            fun <T> value(result: ActorStoreResult<T>): T = when (result) {
                is ActorStoreResult.Value -> result.value
                is ActorStoreResult.Failure -> error(result.message)
            }
            value(
                repository.save(
                    ActorManifest(
                        actorId = "actor",
                        displayName = "Talker",
                        templateId = ActorManifest.CUSTOM_TEMPLATE_ID,
                        currentRevisionId = "revision",
                        createdAtEpochMs = 1,
                        updatedAtEpochMs = 1,
                    ),
                    ActorRevision(
                        revisionId = "revision",
                        actorId = "actor",
                        sceneGraph = graph,
                        artworkAssetPaths = paths,
                        createdAtEpochMs = 1,
                    ),
                ),
            )
            val bundles = ActorBundleRepository(files, repository, PerformanceRepository(files))
            value(bundles.exportActor("actor", "library-actor", "Talker", 2))
            value(bundles.importActor("library-actor", "actor-copy", "revision-copy", 3))
            val copy = value(ActorRepository(ActorFileStore(root)).load("actor-copy")).second
            restored.clear()
            assertTrue(store.restore("actor-copy", copy.artworkAssetPaths, copy.sceneGraph, restored).isEmpty())
            assertEquals(Color.BLUE, restored.getAsset("smile")!!.getPixel(0, 0))
        } finally {
            runtime.clear()
            restored.clear()
            root.deleteRecursively()
        }
    }
}
