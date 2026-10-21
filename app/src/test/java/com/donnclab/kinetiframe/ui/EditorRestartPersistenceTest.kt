package com.donnclab.kinetiframe.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModelStore
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.models.Layer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class EditorRestartPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun heldDrawingScopeResetsOnNavigationAndReferencesSurviveEditorSave() = runTest {
        val directory = temporary.newFolder()
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val writer = FrameCacheManager(directory, writerScope)
            writer.putFrame(0, Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) })
            writer.persistAllToDisk(listOf("main"), 3)
            val main = Layer(name = "Main", id = "main")
            val state =
                TimelineState(
                    project = ProjectDocument(id = "held", stage = StageDimensions(16, 16), layers = listOf(main)),
                    animation = AnimationState(totalFrames = 3),
                    layers = LayerState(layers = listOf(main), frameLayers = (0..2).associateWith { listOf(main) }),
                )
            val editor = TimelineViewModel(directory, engineDispatcher = dispatcher, initialEditorState = state)
            store.put("editor", editor)
            editor.prepareProjectSave()
            editor.handleIntent(TimelineIntent.Frame.HoldDrawing(0, 2))
            val saved = editor.prepareProjectSave()
            assertEquals(3, saved.linkedCels.getValue("main").size)
            editor.handleIntent(TimelineIntent.Frame.EditDrawingScope(0, true))
            editor.prepareProjectSave()
            assertEquals("main" to 0, editor.state.value.layers.linkedEditTarget)
            editor.handleIntent(TimelineIntent.Playback.SetCurrentFrame(1))
            editor.prepareProjectSave()
            assertNull(editor.state.value.layers.linkedEditTarget)
            assertEquals(saved.linkedCels, editor.cacheManager.exposures())
            assertEquals(Color.RED, editor.state.value.currentBitmap!!.getPixel(0, 0))
            editor.handleIntent(TimelineIntent.Frame.EditDrawingScope(1, false))
            val detached = editor.prepareProjectSave()
            assertEquals(setOf(0, 2), detached.linkedCels.getValue("main").keys)
            assertEquals(Color.RED, editor.cacheManager.getFrame(1)!!.getPixel(0, 0))
        } finally {
            writerScope.cancel()
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun reopeningTheEditorLoadsSavedPixelsInsteadOfDeletingThem() = runTest {
        val directory = temporary.newFolder()
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val writer = FrameCacheManager(directory, writerScope)
            writer.putFrame(
                "main",
                0,
                Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.RED)
                },
            )
            writer.putFrame(
                "main",
                1,
                Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.BLUE)
                },
            )
            writer.persistAllToDisk(listOf("main"), 2)
            writerScope.cancel()
            val main = Layer(name = "Main", id = "main")
            val state =
                TimelineState(
                    project = ProjectDocument(id = "saved", stage = StageDimensions(16, 16), layers = listOf(main)),
                    animation = AnimationState(totalFrames = 2),
                    layers = LayerState(
                        layers = listOf(main),
                        frameLayers = mapOf(0 to listOf(main), 1 to listOf(main)),
                    ),
                )
            val editor = TimelineViewModel(directory, engineDispatcher = dispatcher, initialEditorState = state)
            store.put("editor", editor)
            editor.prepareProjectSave()
            assertEquals(Color.RED, editor.state.value.currentBitmap!!.getPixel(0, 0))
            assertEquals(Color.BLUE, editor.cacheManager.getFrame("main", 1)!!.getPixel(0, 0))
            assertTrue(File(directory, "frame_v2_main_frame_0.png").isFile)
            assertTrue(File(directory, "frame_v2_main_frame_1.png").isFile)
        } finally {
            writerScope.cancel()
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun addingAnEmptyLayerDoesNotAllocateOrWriteBlankFrames() = runTest {
        val directory = temporary.newFolder()
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val main = Layer(name = "Main", id = "main")
            val state = TimelineState(
                animation = AnimationState(totalFrames = 500),
                layers = LayerState(layers = listOf(main), frameLayers = mapOf(0 to listOf(main))),
            )
            val editor = TimelineViewModel(directory, engineDispatcher = dispatcher, initialEditorState = state)
            store.put("editor", editor)
            editor.handleIntent(TimelineIntent.Layer.AddLayer())
            val document = editor.prepareProjectSave()
            assertEquals(2, document.frameLayers.getValue(0).size)
            assertEquals(500, document.totalFrames)
            assertEquals(0, directory.listFiles()!!.count { it.extension == "png" })
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
