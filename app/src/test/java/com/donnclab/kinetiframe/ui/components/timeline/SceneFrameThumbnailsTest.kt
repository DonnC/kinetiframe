package com.donnclab.kinetiframe.ui.components.timeline

import android.graphics.Bitmap
import android.graphics.Color
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.ui.LayerState
import com.donnclab.kinetiframe.ui.StudioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Android integration uses Robolectric's JUnit4 runner; pure logic tests use JUnit5. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneFrameThumbnailsTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun thumbnailsIncludeInactiveLayersAndOwnIndependentSmallPixels() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = FrameCacheManager(temporary.newFolder(), scope)
            for (frame in 0..1) {
                val body = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
                body.eraseColor(if (frame == 0) Color.RED else Color.BLUE)
                val paint = android.graphics.Paint().apply { color = if (frame == 0) Color.GREEN else Color.YELLOW }
                android.graphics.Canvas(body).drawRect(256f, 0f, 512f, 256f, paint)
                cache.putFrame("body", frame, body)
                val mouth = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
                mouth.setPixel(0, 0, Color.BLACK)
                cache.putFrame("mouth", frame, mouth)
            }
            val state = StudioState(
                project = ProjectDocument(stage = StageDimensions(512, 256)),
                layers = LayerState(
                    layers = listOf(Layer("Body", id = "body"), Layer("Mouth", id = "mouth", zIndex = 1)),
                    activeLayerIndex = 1,
                ),
            )
            val previews = sceneFrameThumbnails(state, cache, listOf(0, 1))
            assertEquals(256, previews.getValue(0).width)
            assertEquals(Color.RED, previews.getValue(0).getPixel(100, 100))
            assertEquals(Color.BLUE, previews.getValue(1).getPixel(100, 100))
            assertEquals(Color.GREEN, previews.getValue(0).getPixel(200, 100))
            assertEquals(Color.YELLOW, previews.getValue(1).getPixel(200, 100))
            assertNotSame(previews[0], previews[1])
            val missingBackground = state.copy(
                project = state.project.copy(
                    backgroundAssetId = "missing",
                    assets = mapOf(
                        "missing" to com.donnclab.kinetiframe.engine.model.ProjectAsset(
                            "missing",
                            com.donnclab.kinetiframe.engine.model.AssetKind.IMPORTED_IMAGE,
                            "/missing-thumbnail-test.png",
                            512,
                            256,
                        ),
                    ),
                ),
            )
            assertEquals(
                Color.RED,
                sceneFrameThumbnails(missingBackground, cache, listOf(0)).getValue(0).getPixel(100, 100),
            )
            val full = com.donnclab.kinetiframe.domain.render.ProjectFrameCompositor(
                state.project,
                mapOf(0 to state.layers.layers),
                cache,
                null,
                null,
            )
            try {
                assertEquals(512, full.composite(0).width)
                assertEquals(Color.GREEN, full.composite(0).getPixel(400, 200))
            } finally {
                full.release()
            }
            val hidden = state.copy(
                layers = state.layers.copy(
                    layers = state.layers.layers.map {
                        it.copy(isVisible = false)
                    },
                ),
            )
            assertEquals(0, sceneFrameThumbnails(hidden, cache, listOf(0)).getValue(0).getPixel(100, 100))
        } finally {
            scope.cancel()
        }
    }
}
