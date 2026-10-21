package com.donnclab.kinetiframe.domain.render

import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.demo.BundledActorSourceReader
import com.donnclab.kinetiframe.data.demo.DemoAssetFixtures
import com.donnclab.kinetiframe.data.project.KframeArchiver
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActorPlacementPipelineTest {
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `all Mira views reuse render fit and remain visible after repository restart`() {
        val root = Files.createTempDirectory("actor-placement-test").toFile()
        try {
            val files = ActorFileStore(root)
            val assets = File("src/main/assets").takeIf { it.isDirectory } ?: File("app/src/main/assets")
            BundledActorSourceReader.read(
                DemoAssetFixtures.file(assets, BundledActorSourceReader.ASSET_PATH),
                files,
                setOf("ls-mira-all-views-v3"),
            )
            val actorRepository = ActorRepository(files)
            val views = value(actorRepository.load("ls-mira-all-views-v3")).first.authoredViews()
            views.values.forEachIndexed { index, revisionId ->
                value(
                    PerformanceRepository(files).save(
                        PerformanceDocument(
                            performanceId = "mira-view-$index",
                            projectId = "source",
                            actorId = "ls-mira-all-views-v3",
                            actorRevisionId = revisionId,
                            displayName = "Mira view $index",
                            durationMicros = 4_000_000,
                            createdAtEpochMs = 0,
                            updatedAtEpochMs = 0,
                        ),
                    ),
                )
            }
            val repo = PerformanceRepository(files)
            val target = ProjectDocument(id = "destination", stage = StageDimensions(640, 360), fps = 12)
            val actors = ActorRepository(files)
            value(repo.listForActor("ls-mira-all-views-v3")).also { assertEquals(4, it.size) }.forEach { source ->
                val local = value(repo.copyIntoProject(source, target.id))
                val raw = ActorPlacementReference(
                    "placed-${local.performanceId}",
                    local.performanceId,
                    local.actorId,
                    local.actorRevisionId,
                    local.displayName,
                    0,
                    48,
                )
                val prepared = ActorPlacementPreparation(actors, repo, files).prepare(raw, target)
                assertNotEquals(Transform(), prepared.transform)
                val renderer =
                    ActorFrameRenderer(
                        ActorRepository(ActorFileStore(root)),
                        PerformanceRepository(ActorFileStore(root)),
                        files,
                    )
                try {
                    listOf(0, 24, 47).forEach { frame ->
                        val resolved = renderer.resolveAll(listOf(prepared), target.id, frame, target.fps).single()
                        assertNull(resolved.diagnostic)
                        assertTrue(resolved.render!!.commands.isNotEmpty())
                        val points = resolved.render.geometry.quads.flatten()
                        assertFalse(
                            com.donnclab.kinetiframe.domain.actor.ActorStageFraming.outside(points, target.stage),
                        )
                    }
                    val follow = com.donnclab.kinetiframe.engine.camera.CameraShot(
                        "follow",
                        com.donnclab.kinetiframe.engine.camera.CameraAction.FOLLOW,
                        0,
                        48,
                        targetPlacementId = prepared.placementId,
                    )
                    val followedProject = target.copy(
                        cameraShots = listOf(follow),
                        actorPlacements = mapOf(prepared.placementId to prepared),
                    )
                    val finalPose = ActorFollowCamera.evaluate(followedProject, 47, renderer)
                    ActorFollowCamera.evaluate(followedProject, 12, renderer)
                    assertEquals(finalPose, ActorFollowCamera.evaluate(followedProject, 47, renderer))
                    assertEquals(
                        com.donnclab.kinetiframe.engine.camera.CameraPose(),
                        ActorFollowCamera.evaluate(followedProject, 48, renderer),
                    )
                    assertNull(renderer.resolveAll(listOf(prepared), target.id, 48, 12).single().render)
                } finally {
                    renderer.clear()
                }
            }
            val preview = value(actors.characterPreview("ls-mira-all-views-v3"))!!
            val bitmap = BitmapFactory.decodeFile(preview)
            assertEquals(320, bitmap.width)
            assertEquals(320, bitmap.height)
            bitmap.recycle()
        } finally {
            root.deleteRecursively()
        }
    }
}
