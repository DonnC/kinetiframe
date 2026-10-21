package com.donnclab.kinetiframe.data.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.project.KframeArchiver
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.domain.render.ActorFrameRenderer
import com.donnclab.kinetiframe.domain.render.ProjectFrameCompositor
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Robolectric needs its JUnit4 runner; pure storage/math criteria live in the JUnit5 suite. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BundledDemoRenderTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun everyDemoRendersDistinctStageSizedFramesThroughTheRealExportCompositor() = runBlocking {
        renderProjects(BundledDemoInstaller.projectIds)
    }

    @Test fun taviProductionRendersEditableLayersAtHdStageSize() = runBlocking {
        renderProjects(listOf(BundledDemoInstaller.TAVI_PRESENTATION_ID))
    }

    private suspend fun renderProjects(projectIds: List<String>) {
        val files = ActorFileStore(temporary.newFolder())
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        projectIds.forEach { id ->
            val path = BundledDemoInstaller.projectAssetPath(id)
            assertEquals(
                id,
                value(
                    KframeArchiver.import(
                        files,
                        com.donnclab.kinetiframe.data.demo.DemoAssetFixtures.file(assets, path),
                    ),
                ),
            )
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val proofRoot = File("../.artifacts/demos/android-render-proofs").apply { mkdirs() }
        try {
            projectIds.forEach { id ->
                val (_, saved) = value(ProjectRepository(files).load(id))
                val project = saved.project
                val background = BitmapFactory.decodeFile(project.assets.getValue("stage").sourcePath)
                assertNotNull(background)
                val renderer = ActorFrameRenderer(ActorRepository(files), PerformanceRepository(files), files)
                val cache = FrameCacheManager(File(files.projectDirectory(id), "frames"), scope)
                cache.restoreExposures(saved.linkedCels, saved.totalFrames)
                val compositor = ProjectFrameCompositor(project, saved.frameLayers, cache, renderer, background)
                try {
                    val hashes = mutableSetOf<Int>()
                    val movie = System.getenv("LS_RENDER_MOVIE") == "1"
                    val reviewFrames = if (movie) {
                        (0 until saved.totalFrames).toList()
                    } else if (id.startsWith("ls-cast-all-views")) {
                        listOf(0, 36, 96, 132, 192, 228, 288, 324, 383)
                    } else {
                        listOf(
                            0,
                            saved.totalFrames / 4,
                            saved.totalFrames / 2,
                            saved.totalFrames * 3 / 4,
                            saved.totalFrames - 1,
                        )
                    }
                    val movieRoot = File(proofRoot, id).apply { if (movie) mkdirs() }
                    val markers = setOf(
                        0,
                        saved.totalFrames / 4,
                        saved.totalFrames / 2,
                        saved.totalFrames * 3 / 4,
                        saved.totalFrames - 1,
                    )
                    reviewFrames.forEach { frame ->
                        assertTrue(
                            "Actor diagnostic at $id/$frame",
                            renderer.resolveAll(
                                project.actorPlacements.values,
                                id,
                                frame,
                                project.fps,
                            ).none { it.diagnostic != null },
                        )
                        val bitmap = compositor.composite(frame)
                        assertEquals(project.stage.width, bitmap.width)
                        assertEquals(project.stage.height, bitmap.height)
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        hashes += pixels.contentHashCode()
                        assertTrue(
                            "No visible ink for $id/$frame",
                            pixels.count { pixel ->
                                android.graphics.Color.red(pixel) < 80 && android.graphics.Color.green(pixel) < 100 &&
                                    android.graphics.Color.blue(pixel) < 100
                            } > 150,
                        )
                        if (!movie ||
                            frame in markers
                        ) {
                            File(proofRoot, "$id-$frame.png").outputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                        }
                        if (movie) {
                            File(movieRoot, "%04d.png".format(frame)).outputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                        }
                    }
                    assertTrue("No changing motion in $id", hashes.size >= 3)
                } finally {
                    compositor.release()
                    background?.recycle()
                }
            }
        } finally {
            scope.cancel()
        }
    }
}
