// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModelStore
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.TouchPoint
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Frame inserts, deletes and pastes must settle the drawing on the canvas before frames shift.
 * Otherwise the next frame load re-bakes the outgoing drawing at its old index (R3-18).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrameStructureDrawingTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun deletingTheLastFrameAfterDrawingOnItDoesNotBringItBackInAnAddedFrame() = runTest {
        withEditor(listOf(Color.BLUE, Color.RED), currentFrame = 1) { editor ->
            editor.handleIntent(StudioIntent.Draw.BakeStroke(stroke()))
            editor.prepareProjectSave()

            editor.handleIntent(StudioIntent.Frame.DeleteFrames(listOf(1)))
            editor.prepareProjectSave()
            assertNull(editor.cacheManager.getFrame("main", 1))

            editor.handleIntent(StudioIntent.Frame.AddFrame)
            editor.prepareProjectSave()
            assertEquals(2, editor.state.value.animation.totalFrames)
            assertNull(editor.cacheManager.getFrame("main", 1))
        }
    }

    @Test fun deletingAMiddleFrameAfterDrawingOnItKeepsTheFrameThatMovesIntoItsPlace() = runTest {
        withEditor(listOf(Color.BLUE, Color.RED, Color.GREEN), currentFrame = 1) { editor ->
            editor.handleIntent(StudioIntent.Draw.BakeStroke(stroke()))
            editor.prepareProjectSave()

            editor.handleIntent(StudioIntent.Frame.DeleteFrames(listOf(1)))
            editor.prepareProjectSave()

            assertEquals(2, editor.state.value.animation.totalFrames)
            assertEquals(Color.GREEN, editor.cacheManager.getFrame("main", 1)!!.getPixel(0, 0))
        }
    }

    @Test fun pastingBeforeTheFrameBeingDrawnKeepsThePastedFrame() = runTest {
        withEditor(listOf(Color.BLUE, Color.RED), currentFrame = 1) { editor ->
            editor.handleIntent(StudioIntent.Frame.CopyFrame(0))
            editor.handleIntent(StudioIntent.Draw.BakeStroke(stroke()))
            editor.prepareProjectSave()

            editor.handleIntent(StudioIntent.Frame.PasteFrame(1, before = true))
            editor.prepareProjectSave()

            assertEquals(3, editor.state.value.animation.totalFrames)
            assertEquals(Color.BLUE, editor.cacheManager.getFrame("main", 1)!!.getPixel(0, 0))
            assertEquals(Color.RED, editor.cacheManager.getFrame("main", 2)!!.getPixel(0, 0))
        }
    }

    @Test fun movingAFrameShiftsTheFramesBetween() = runTest {
        withEditor(listOf(Color.BLUE, Color.RED, Color.GREEN), currentFrame = 0) { editor ->
            editor.handleIntent(StudioIntent.Frame.MoveFrame(0, 2))
            editor.prepareProjectSave()

            assertEquals(3, editor.state.value.animation.totalFrames)
            val colours = (0..2).map { editor.cacheManager.getFrame("main", it)!!.getPixel(0, 0) }
            assertEquals(listOf(Color.RED, Color.GREEN, Color.BLUE), colours)
        }
    }

    /** A short mark in the far corner, so pixel (0, 0) still shows the frame's fill colour. */
    private fun stroke() = listOf(TouchPoint(12f, 12f, timestamp = 1), TouchPoint(14f, 14f, timestamp = 2))

    private suspend fun TestScope.withEditor(
        frameColours: List<Int>,
        currentFrame: Int,
        block: suspend (StudioViewModel) -> Unit,
    ) {
        val directory = temporary.newFolder()
        writeFrames(directory, frameColours)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val main = Layer(name = "Main", id = "main")
            val frames = frameColours.indices
            val state = StudioState(
                project = ProjectDocument(id = "frames", stage = StageDimensions(SIZE, SIZE), layers = listOf(main)),
                animation = AnimationState(totalFrames = frameColours.size, currentFrame = currentFrame),
                layers = LayerState(layers = listOf(main), frameLayers = frames.associateWith { listOf(main) }),
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

    private suspend fun writeFrames(directory: File, colours: List<Int>) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val writer = FrameCacheManager(directory, scope)
            colours.forEachIndexed { index, colour ->
                writer.putFrame(
                    "main",
                    index,
                    Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(colour)
                    },
                )
            }
            writer.persistAllToDisk(listOf("main"), colours.size)
        } finally {
            scope.cancel()
        }
    }

    private companion object {
        const val SIZE = 16
    }
}
