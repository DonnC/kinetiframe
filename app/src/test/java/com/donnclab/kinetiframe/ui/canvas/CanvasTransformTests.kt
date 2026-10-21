package com.donnclab.kinetiframe.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CanvasTransformTests {
    @Test fun reopenedCameraProjectCentersStageInBothPortraitAndLandscapeWorkspaces() {
        val document = com.donnclab.kinetiframe.engine.model.ProjectDocument(
            stage = com.donnclab.kinetiframe.engine.model.StageDimensions(1280, 720),
            cameraShots = listOf(
                com.donnclab.kinetiframe.engine.camera.CameraShot(
                    "fade",
                    com.donnclab.kinetiframe.engine.camera.CameraAction.FADE_IN,
                    0,
                    12,
                ),
            ),
        )
        var state = com.donnclab.kinetiframe.ui.TimelineState(project = document)
        val manager = com.donnclab.kinetiframe.domain.manager.ViewManager { change -> state = change(state) }
        for ((width, height) in listOf(1080 to 1600, 1600 to 900)) {
            manager.setViewportSize(width, height)
            val center = floatArrayOf(640f, 360f)
            android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) }.mapPoints(center)
            assertEquals(width / 2f, center[0], .001f)
            assertEquals(height / 2f, center[1], .001f)
            assertEquals(document.cameraShots, state.project.cameraShots)
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TimelineViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val tempDir = File.createTempFile("kinetiframe_transform_test", "")
        tempDir.delete()
        tempDir.mkdir()
        viewModel = TimelineViewModel(tempDir, testDispatcher, shouldInitialize = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pivotIntegrityTest() = runTest {
        val initialOffset = Offset.Zero
        val zoomDelta = 2.0f
        val pivot = Offset(500f, 500f)

        val expectedOffset = pivot - (pivot - initialOffset) * zoomDelta

        val matrix = android.graphics.Matrix().apply {
            postScale(zoomDelta, zoomDelta, pivot.x, pivot.y)
        }
        viewModel.handleIntent(TimelineIntent.View.UpdateCanvasTransform(matrix))
        testDispatcher.scheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        val values = currentState.canvas.matrixValues

        assertEquals(2.0f, values[android.graphics.Matrix.MSCALE_X], 0.001f)
        assertEquals(expectedOffset.x, values[android.graphics.Matrix.MTRANS_X], 0.001f)
        assertEquals(expectedOffset.y, values[android.graphics.Matrix.MTRANS_Y], 0.001f)
    }

    @Test
    fun frameNavigationDoesNotChangeTheEditorViewport() = runTest {
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(1))
        testDispatcher.scheduler.advanceUntilIdle()
        val matrix = android.graphics.Matrix().apply {
            postScale(2.0f, 2.0f)
            postTranslate(100f, 100f)
        }
        viewModel.handleIntent(TimelineIntent.View.UpdateCanvasTransform(matrix))
        testDispatcher.scheduler.advanceUntilIdle()

        val expected = viewModel.state.value.canvas

        viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val afterFirstSwitch = viewModel.state.value.canvas
        assertEquals(expected.zoom, afterFirstSwitch.zoom, 0.001f)
        assertEquals(expected.pan.x, afterFirstSwitch.pan.x, 0.001f)
        assertEquals(expected.pan.y, afterFirstSwitch.pan.y, 0.001f)

        viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(1))
        testDispatcher.scheduler.advanceUntilIdle()

        val afterSecondSwitch = viewModel.state.value.canvas
        assertEquals(expected.zoom, afterSecondSwitch.zoom, 0.001f)
        assertEquals(expected.pan.x, afterSecondSwitch.pan.x, 0.001f)
        assertEquals(expected.pan.y, afterSecondSwitch.pan.y, 0.001f)
    }

    @Test
    fun repeatedFrameSelectionPreservesFittedZoomAndPan() = runTest {
        viewModel.handleIntent(TimelineIntent.View.SetViewportSize(width = 1_000, height = 1_000))
        viewModel.handleIntent(
            TimelineIntent.View.UpdateTransform(
                panDelta = Offset(48f, -24f),
                zoomDelta = 1.5f,
                rotationDelta = 0f,
                pivot = Offset(500f, 500f),
            ),
        )
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()

        val expected = viewModel.state.value.canvas
        repeat(3) {
            viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(0))
            viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(1))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val actual = viewModel.state.value.canvas
        assertEquals(expected.zoom, actual.zoom, 0.001f)
        assertEquals(expected.pan.x, actual.pan.x, 0.001f)
        assertEquals(expected.pan.y, actual.pan.y, 0.001f)
        assertEquals(expected.rotation, actual.rotation, 0.001f)
        actual.matrixValues.indices.forEach { index ->
            assertEquals(expected.matrixValues[index], actual.matrixValues[index], 0.001f)
        }
    }

    @Test
    fun resetReliabilityTest() = runTest {
        val matrix = android.graphics.Matrix().apply {
            postScale(3.5f, 3.5f)
            postTranslate(-400f, 600f)
            postRotate(45f)
        }
        viewModel.handleIntent(TimelineIntent.View.UpdateCanvasTransform(matrix))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotEquals(1.0f, viewModel.state.value.canvas.zoom)

        viewModel.handleIntent(TimelineIntent.View.ResetCanvasTransform)
        testDispatcher.scheduler.advanceUntilIdle()

        val stateReset = viewModel.state.value
        assertEquals(1.0f, stateReset.canvas.zoom, 0.001f)
        assertEquals(0f, stateReset.canvas.pan.x, 0.001f)
        assertEquals(0f, stateReset.canvas.rotation, 0.001f)
    }

    @Test
    fun viewportSizeFitsDocumentIntoAvailableEditorSpace() = runTest {
        viewModel.handleIntent(TimelineIntent.View.SetViewportSize(width = 1_000, height = 1_000))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        val matrix = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) }
        val documentCorners = floatArrayOf(0f, 0f, 1_920f, 1_080f)
        matrix.mapPoints(documentCorners)

        // 16:9 fits a square viewport at 1,000px wide, with the remaining
        // vertical space becoming non-writable letterboxing above and below.
        assertEquals(1f, state.canvas.zoom, 0.001f)
        assertEquals(0f, documentCorners[0], 0.001f)
        assertEquals(218.75f, documentCorners[1], 0.001f)
        assertEquals(1_000f, documentCorners[2], 0.001f)
        assertEquals(781.25f, documentCorners[3], 0.001f)
    }
}
