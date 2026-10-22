package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.StageDimensions
import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
class SceneVideoCompositorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun sceneVideoUsesFrameRangeAndPlacementWhileReferenceStaysOutOfExport() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val cache = FrameCacheManager(temporary.newFolder(), scope)
        val clip = VideoClipPlacement(
            "clip", "asset", "TV", 1, 3, isSceneContent = true,
            x = 20f, y = 20f, width = 40f, height = 20f,
        )
        val project = ProjectDocument(stage = StageDimensions(100, 100), videoClips = mapOf("clip" to clip))
        val decoded = mutableListOf<Int>()
        val compositor = ProjectFrameCompositor(project, emptyMap(), cache, null, null, videoFrameAt = { frame ->
            decoded.add(frame)
            Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply {
                eraseColor(
                    if (frame ==
                        1
                    ) {
                        Color.RED
                    } else {
                        Color.BLUE
                    },
                )
            }
        })
        try {
            assertEquals(Color.TRANSPARENT, compositor.composite(0).getPixel(30, 30))
            assertEquals(Color.RED, compositor.composite(1).getPixel(30, 30))
            assertEquals(Color.TRANSPARENT, compositor.composite(1).getPixel(5, 5))
            assertEquals(Color.BLUE, compositor.composite(2).getPixel(30, 30))
            assertEquals(Color.TRANSPARENT, compositor.composite(3).getPixel(30, 30))
            assertEquals(listOf(1, 1, 2), decoded)
            val reference =
                ProjectFrameCompositor(
                    project.copy(videoClips = mapOf("clip" to clip.copy(isSceneContent = false))),
                    emptyMap(),
                    cache,
                    null,
                    null,
                    videoFrameAt = {
                        throw AssertionError("Must not decode tracing references")
                    },
                )
            try {
                assertEquals(Color.TRANSPARENT, reference.composite(1).getPixel(30, 30))
            } finally {
                reference.release()
            }
            val missing = ProjectFrameCompositor(project, emptyMap(), cache, null, null, videoFrameAt = { null })
            try {
                try {
                    missing.composite(1)
                    fail("Missing scene video must fail export explicitly")
                } catch (
                    expected: IllegalStateException,
                ) {
                    assertTrue(expected.message.orEmpty().contains("TV"))
                }
            } finally {
                missing.release()
            }
        } finally {
            compositor.release()
            scope.cancel()
        }
    }
}
