package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.document.VideoFit
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.studio.camera.SceneCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneCameraCompositorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun videoFillUsesTheChosenSourceEdgeRatherThanStretching() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val clip = VideoClipPlacement(
            "v", "asset", "Wide video", 0, 24, isSceneContent = true,
            width = 100f, height = 100f, fit = VideoFit.FILL, cropX = 1f,
        )
        val project = ProjectDocument(stage = StageDimensions(100, 100), videoClips = mapOf("v" to clip))
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, null, videoFrameAt = {
            Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
                android.graphics.Canvas(this).drawRect(
                    100f,
                    0f,
                    200f,
                    100f,
                    android.graphics.Paint().apply {
                        color =
                            Color.BLUE
                    },
                )
            }
        })
        try {
            assertEquals(Color.BLUE, compositor.composite(0).getPixel(10, 50))
            assertEquals(Color.BLUE, compositor.composite(0).getPixel(90, 50))
        } finally {
            compositor.release()
            scope.cancel()
        }
    }

    @Test fun impactShakeHasNoRenderedResidueAfterFrame21EvenWhenScrubbingBackward() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val background = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val shot = SceneCamera.preset("impact", CameraAction.SHAKE, 8, 21, CameraPose(), 1f)
        val project = cameraProject(CameraPose()).copy(fps = 12, cameraShots = listOf(shot))
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, background)
        fun pixels(bitmap: Bitmap) = IntArray(10000).also { bitmap.getPixels(it, 0, 100, 0, 0, 100, 100) }
        try {
            val baseline = pixels(compositor.composite(0))
            compositor.composite(12)
            for (frame in listOf(21, 40, 95, 22)) assertArrayEquals(baseline, pixels(compositor.composite(frame)))
            compositor.composite(15)
            assertArrayEquals(baseline, pixels(compositor.composite(21)))
        } finally {
            compositor.release()
            background.recycle()
            scope.cancel()
        }
    }

    @Test fun galleryPosterRemainsVisibleWithoutRemovingFadesFromActualScene() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val background = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val project = cameraProject(CameraPose(black = 1f))
        val scene = ProjectFrameCompositor(project, emptyMap(), cache, null, background)
        val poster = ProjectFrameCompositor(project, emptyMap(), cache, null, background, includeFades = false)
        try {
            assertEquals(Color.BLACK, scene.composite(0).getPixel(50, 50))
            assertEquals(Color.RED, poster.composite(0).getPixel(50, 50))
        } finally {
            scene.release()
            poster.release()
            background.recycle()
            scope.cancel()
        }
    }
    private fun cameraProject(pose: CameraPose) = ProjectDocument(
        stage = StageDimensions(100, 100),
        cameraShots = listOf(CameraShot("shot", CameraAction.PAN, 0, 2, pose, pose)),
    )

    @Test fun cameraRevealsOffstageVideoWithoutFlatteningStageFirst() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val clip = VideoClipPlacement(
            "v", "asset", "Screen", 0, 24, isSceneContent = true,
            x = 120f, y = 40f, width = 40f, height = 20f,
        )
        val project = cameraProject(CameraPose(x = 1.4f)).copy(videoClips = mapOf("v" to clip))
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, null, videoFrameAt = {
            Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        })
        try {
            val first = compositor.composite(0)
            assertEquals(Color.RED, first.getPixel(50, 50))
            assertEquals(Color.BLACK, first.getPixel(90, 20))
            assertSame(first, compositor.composite(1))
        } finally {
            compositor.release()
            scope.cancel()
        }
    }

    @Test fun pullOutExposesBlackMatteWhilePaperRemainsWhite() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val compositor = ProjectFrameCompositor(cameraProject(CameraPose(zoom = .5f)), emptyMap(), cache, null, null)
        try {
            val frame = compositor.composite(0)
            assertEquals(Color.WHITE, frame.getPixel(50, 50))
            assertEquals(Color.BLACK, frame.getPixel(5, 5))
        } finally {
            compositor.release()
            scope.cancel()
        }
    }

    @Test fun fadeIsAppliedAfterSceneAndAtReducedThumbnailResolution() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val background = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val project = cameraProject(CameraPose(black = 1f))
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, background, outputMaxEdge = 25)
        try {
            val frame = compositor.composite(0)
            assertEquals(25, frame.width)
            assertEquals(Color.BLACK, frame.getPixel(12, 12))
            assertEquals(Color.RED, compositor.composite(3).getPixel(12, 12))
        } finally {
            compositor.release()
            background.recycle()
            scope.cancel()
        }
    }

    @Test fun cameraSaveRestoreDoesNotAccumulateAcrossOutOfOrderScrubs() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val clip = VideoClipPlacement(
            "v", "asset", "Screen", 0, 24, isSceneContent = true,
            x = 40f, y = 40f, width = 20f, height = 20f,
        )
        val shot = CameraShot("pan", CameraAction.PAN, 0, 3, CameraPose(), CameraPose(x = .7f))
        val project = cameraProject(CameraPose()).copy(cameraShots = listOf(shot), videoClips = mapOf("v" to clip))
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, null, videoFrameAt = {
            Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        })
        try {
            assertEquals(Color.RED, compositor.composite(0).getPixel(50, 50))
            assertEquals(Color.RED, compositor.composite(2).getPixel(30, 50))
            assertEquals(Color.RED, compositor.composite(0).getPixel(50, 50))
            assertEquals(Color.WHITE, compositor.composite(0).getPixel(30, 50))
        } finally {
            compositor.release()
            scope.cancel()
        }
    }
}
