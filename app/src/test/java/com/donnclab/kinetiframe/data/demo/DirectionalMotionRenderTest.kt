package com.donnclab.kinetiframe.data.demo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.actor.validate
import com.donnclab.kinetiframe.data.project.KframeArchiver
import com.donnclab.kinetiframe.domain.render.ActorFrameRenderer
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Visual acceptance uses the same resolver, pose math and artwork renderer as editor/export. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DirectionalMotionRenderTest {
    @Test fun joinedStarterHeadsAndHandsRenderAllViewsAndNativeBindsMatch() {
        withCast { actors, performances, renderer ->
            java.util.zip.ZipFile(
                com.donnclab.kinetiframe.data.demo.DemoAssetFixtures.file(assets, BundledActorSourceReader.ASSET_PATH),
            ).use { archive ->
                listOf("tavi", "mira", "kuda").forEach { character ->
                    val sheet = Bitmap.createBitmap(960, 1600, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(sheet).apply { drawColor(Color.WHITE) }
                    listOf("front", "profile", "three_quarter", "back").forEachIndexed { row, view ->
                        val actorId = "ls-$character-all-views-v3"
                        val revision = value(actors.loadRevision(actorId, "$actorId-$view-joined-v4"))
                        val binding = revision.sceneGraph.bones.getValue("head").textureBinding!!
                        val stored = binding.skin!!
                        assertTrue(stored.attachmentBlend)
                        val path = "dependencies/actors/$actorId/" + revision.artworkAssetPaths.getValue("head")
                        val pixels = archive.getInputStream(archive.getEntry(path)).use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }!!
                        val native = try {
                            com.donnclab.kinetiframe.engine.rigging.math.ArtworkAttachmentBlend.bind(
                                revision.sceneGraph,
                                "head",
                                binding,
                                pixels.width,
                                pixels.height,
                            )
                        } finally {
                            pixels.recycle()
                        }
                        assertEquals(native.vertices.size, stored.vertices.size)
                        native.vertices.zip(stored.vertices).forEach { (a, b) ->
                            assertEquals(a.x, b.x, .005f)
                            assertEquals(a.y, b.y, .005f)
                            a.weights.zip(b.weights).forEach { (x, y) -> assertEquals(x, y, .0001f) }
                        }
                        listOf(-30f, 0f, 30f).forEachIndexed { column, angle ->
                            val document =
                                PerformanceDocument(
                                    performanceId = "$character-$view-joined-$column",
                                    projectId = "review",
                                    actorId = actorId,
                                    actorRevisionId = revision.revisionId,
                                    displayName = "$view $angle",
                                    durationMicros = 1_000_000,
                                    createdAtEpochMs = 2,
                                    updatedAtEpochMs = 2,
                                    poseKeys = listOf(ActorPoseKey(0, mapOf("head" to angle))),
                                    artworkKeys = listOf(
                                        ActorArtworkKey(0, mapOf("right_hand" to listOf("", "palm", "fist")[column])),
                                    ),
                                )
                            value(performances.save(document))
                            drawCell(canvas, renderer, document, 0, column, row, "$view $angle")
                        }
                    }
                    save(sheet, "$character-head-hands", "joined-v4")
                }
            }
        }
    }

    @get:Rule val temporary = TemporaryFolder()
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private val json = Json { ignoreUnknownKeys = true }
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun viewSpecificHandsAndHeadTiltsRenderWithoutMissingAssets() {
        withCast { actors, performances, renderer ->
            for (character in listOf("tavi", "mira", "kuda")) {
                val sheet = Bitmap.createBitmap(7 * 320, 4 * 400, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(sheet).apply { drawColor(Color.WHITE) }
                listOf("front", "profile", "three_quarter", "back").forEachIndexed { row, view ->
                    val actorId = "ls-$character-all-views-v3"
                    val revision = value(actors.loadRevision(actorId, "$actorId-$view-joined-v4"))
                    listOf("", "palm", "point", "fist", "tilt-30", "tilt0", "tilt30").forEachIndexed { column, mode ->
                        val angle = mode.removePrefix("tilt").toFloatOrNull()
                        val document =
                            PerformanceDocument(
                                performanceId = "$character-$view-$column", projectId = "review",
                                actorId = actorId, actorRevisionId = revision.revisionId, displayName = "$view $mode",
                                durationMicros = 1_000_000, createdAtEpochMs = 1, updatedAtEpochMs = 1,
                                poseKeys = if (angle ==
                                    null
                                ) {
                                    emptyList()
                                } else {
                                    listOf(ActorPoseKey(0, mapOf("head" to angle)))
                                },
                                artworkKeys = if (angle != null) {
                                    emptyList()
                                } else {
                                    listOf(
                                        ActorArtworkKey(
                                            0,
                                            mapOf("left_hand" to mode, "right_hand" to mode),
                                        ),
                                    )
                                },
                            )
                        value(performances.save(document))
                        drawCell(canvas, renderer, document, 0, column, row, "$view ${mode.ifEmpty { "rest" }}")
                    }
                }
                save(sheet, "$character-hands-head-tilts")
            }
        }
    }

    @Test fun shippingProfileWalkRunRenderAcrossWholeCycleForEveryStarter() {
        withCast { actors, performances, renderer ->
            val sheet = Bitmap.createBitmap(8 * 320, 6 * 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sheet).apply { drawColor(Color.WHITE) }
            listOf("tavi", "mira", "kuda").forEachIndexed { actorIndex, character ->
                val actorId = "ls-$character-all-views-v3"
                val revision = value(actors.loadRevision(actorId, "$actorId-profile-joined-v4"))
                listOf("walk", "run").forEachIndexed { index, motion ->
                    val recipe = json.decodeFromString<ActorMotionPreset>(
                        com.donnclab.kinetiframe.data.demo.DemoAssetFixtures.file(
                            assets,
                            "actor/presets/views/profile-$motion.json",
                        ).readText(),
                    ).validate()
                    assertTrue(recipe.supportsView(revision.view))
                    assertTrue(recipe.missingBones(revision.sceneGraph.bones.keys).isEmpty())
                    val applied = AppliedActorPreset(recipe)
                    val document =
                        PerformanceDocument(
                            performanceId = "$character-$motion", projectId = "review", actorId = actorId,
                            actorRevisionId = revision.revisionId,
                            displayName = motion,
                            durationMicros = applied.durationMicros,
                            presetSnapshot = applied, createdAtEpochMs = 1, updatedAtEpochMs = 1,
                        )
                    value(performances.save(document))
                    repeat(8) { phase ->
                        drawCell(
                            canvas,
                            renderer,
                            document,
                            phase * applied.durationMicros / 8,
                            phase,
                            actorIndex * 2 + index,
                            "$character $motion $phase/8",
                        )
                    }
                }
            }
            save(sheet, "profile-walk-run-cycles")
        }
    }

    private fun withCast(block: (ActorRepository, PerformanceRepository, ActorFrameRenderer) -> Unit) {
        val files = ActorFileStore(temporary.newFolder())
        BundledActorSourceReader.read(
            DemoAssetFixtures.file(assets, BundledActorSourceReader.ASSET_PATH),
            files,
            listOf("tavi", "mira", "kuda").map { "ls-$it-all-views-v3" }.toSet(),
        )
        val actors = ActorRepository(files)
        val performances = PerformanceRepository(files)
        val renderer = ActorFrameRenderer(actors, performances, files)
        try {
            block(actors, performances, renderer)
        } finally {
            renderer.clear()
        }
    }

    private fun drawCell(
        canvas: Canvas,
        renderer: ActorFrameRenderer,
        document: PerformanceDocument,
        time: Long,
        column: Int,
        row: Int,
        label: String,
    ) {
        val placement =
            ActorPlacementReference(
                placementId = document.performanceId,
                performanceId = document.performanceId,
                actorId = document.actorId,
                actorRevisionId = document.actorRevisionId,
                label = label,
                startFrame = 0,
                durationFrames = 240,
                transform = Transform(
                    translation = com.donnclab.kinetiframe.core.geometry.Point2(-464f, -145f),
                    rotationDegrees = 0f,
                    stretch = com.donnclab.kinetiframe.core.geometry.Point2(.65f, .65f),
                ),
            )
        val resolution = renderer.resolveAll(listOf(placement), "review", (time * 24 / 1_000_000).toInt(), 24).single()
        assertNull("$label: ${resolution.diagnostic}", resolution.diagnostic)
        val render = requireNotNull(resolution.render)
        assertTrue(render.assets.isNotEmpty())
        canvas.save()
        canvas.translate(column * 320f, row * 400f)
        canvas.clipRect(0f, 0f, 320f, 400f)
        renderer.draw(canvas, Matrix(), listOf(render))
        canvas.drawText(
            label,
            8f,
            24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 14f
            },
        )
        canvas.restore()
    }

    private fun save(bitmap: Bitmap, name: String, folder: String = "directional-v3") {
        try {
            val proof = File("../.artifacts/demos/android-render-proofs/$folder").apply { mkdirs() }
            File(proof, "$name.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
