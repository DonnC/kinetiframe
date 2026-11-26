// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModelStore
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.studio.AnimationState
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.StudioDirectories
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** R3-27: duplicating a layer copies it on every frame; merging folds it into the layer below. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayerContentTest {
    @get:Rule val temporary = TemporaryFolder()

    private val back = Layer(name = "Back", id = "main", zIndex = 0)
    private val ink = Layer(name = "Ink", id = "ink", zIndex = 1)

    @Test fun duplicatingALayerCopiesItsDrawingOnEveryFrame() = runTest {
        withEditor { editor ->
            editor.handleIntent(StudioIntent.Layer.DuplicateLayer(1))
            editor.prepareProjectSave()

            val stack = editor.state.value.layers.layers
            assertEquals(listOf("Back", "Ink", "Ink copy"), stack.map { it.name })
            assertEquals(2, editor.state.value.layers.activeLayerIndex)
            repeat(FRAMES) { frame ->
                assertEquals(Color.RED, editor.cacheManager.getFrame(stack[2].id, frame)!!.getPixel(1, 4))
            }
        }
    }

    @Test fun mergingDownFoldsTheLayerIntoTheOneBelowOnEveryFrame() = runTest {
        withEditor { editor ->
            editor.handleIntent(StudioIntent.Layer.MergeLayerDown(1))
            editor.prepareProjectSave()

            assertEquals(listOf("Back"), editor.state.value.layers.layers.map { it.name })
            repeat(FRAMES) { frame ->
                val merged = editor.cacheManager.getFrame("main", frame)!!
                assertEquals(Color.RED, merged.getPixel(1, 4))
                assertEquals(Color.BLUE, merged.getPixel(6, 4))
            }
        }
    }

    /** Back is blue everywhere; ink is red on its left half, on both frames. */
    private suspend fun TestScope.withEditor(block: suspend (StudioViewModel) -> Unit) {
        val directory = temporary.newFolder()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val writer = FrameCacheManager(directory, scope)
            repeat(FRAMES) { frame ->
                writer.putFrame(
                    "main",
                    frame,
                    Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.BLUE)
                    },
                )
                writer.putFrame(
                    "ink",
                    frame,
                    Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
                        for (x in 0 until SIZE / 2) for (y in 0 until SIZE) setPixel(x, y, Color.RED)
                    },
                )
            }
            writer.persistAllToDisk(listOf("main", "ink"), FRAMES)
        } finally {
            scope.cancel()
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val stack = listOf(back, ink)
            val state = StudioState(
                project = ProjectDocument(id = "layers", stage = StageDimensions(SIZE, SIZE), layers = stack),
                animation = AnimationState(totalFrames = FRAMES),
                layers = LayerState(layers = stack, frameLayers = (0 until FRAMES).associateWith { stack }),
            )
            val editor =
                StudioViewModel(StudioDirectories(directory), engineDispatcher = dispatcher, initialEditorState = state)
            store.put("editor", editor)
            editor.prepareProjectSave()
            block(editor)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val SIZE = 8
        const val FRAMES = 2
    }
}
