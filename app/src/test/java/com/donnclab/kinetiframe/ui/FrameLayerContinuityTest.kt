package com.donnclab.kinetiframe.ui

import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.models.Layer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class FrameLayerContinuityTest {
    @Test fun `third blank frame retains selected layer and order`(@TempDir directory: Path) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val back = Layer(id = "main", name = "Back", zIndex = 0)
            val front = Layer(id = "ink", name = "Ink", zIndex = 1)
            val stack = listOf(back, front)
            val editor = StudioViewModel(
                cacheDir = directory.toFile(),
                engineDispatcher = dispatcher,
                shouldInitialize = false,
                initialEditorState = StudioState(
                    project = ProjectDocument(layers = stack),
                    layers = LayerState(layers = stack, frameLayers = mapOf(0 to stack), activeLayerIndex = 1),
                ),
            )
            // Queue both taps before either asynchronous insertion runs.
            repeat(2) { editor.handleIntent(StudioIntent.Frame.AddFrame) }
            testScheduler.advanceUntilIdle()
            val state = editor.state.value
            assertEquals(2, state.animation.currentFrame)
            assertEquals(3, state.animation.totalFrames)
            assertEquals(listOf("main", "ink"), state.currentFrameLayers().map(Layer::id))
            assertEquals("ink", state.currentFrameLayers()[state.layers.activeLayerIndex].id)
            assertEquals((0..2).toList(), state.layers.frameLayers.keys.sorted())
        } finally {
            Dispatchers.resetMain()
        }
    }
}
