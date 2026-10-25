package com.donnclab.kinetiframe.studio

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.DrawingTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: StudioViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Use a temporary directory for the cache manager in tests
        val tempDir = java.io.File.createTempFile("kinetiframe_test_cache", "")
        tempDir.delete()
        tempDir.mkdir()
        viewModel = StudioViewModel(tempDir, testDispatcher, shouldInitialize = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state.value
        assertEquals(0, state.animation.currentFrame)
        assertEquals(1, state.animation.totalFrames)
        assertFalse(state.animation.isPlaying)
        assertEquals(12, state.animation.fps) // Default was 12 in the new AnimationState
    }

    @Test
    fun `pack tags commit with insertions and deletions`() = runTest {
        val head = com.donnclab.kinetiframe.library.model.PackFrameTag("Head")
        val hand = com.donnclab.kinetiframe.library.model.PackFrameTag("Hand")
        val eye = com.donnclab.kinetiframe.library.model.PackFrameTag("Eye")
        viewModel.handleIntent(StudioIntent.PackItem.SetTag(0, head))
        viewModel.handleIntent(StudioIntent.PackItem.Add(1, hand))
        viewModel.prepareProjectSave()
        assertEquals(mapOf(0 to head, 1 to hand), viewModel.state.value.packTags)
        viewModel.handleIntent(StudioIntent.PackItem.Add(0, eye))
        viewModel.prepareProjectSave()
        assertEquals(mapOf(0 to eye, 1 to head, 2 to hand), viewModel.state.value.packTags)
        viewModel.handleIntent(StudioIntent.Frame.DeleteFrame(1))
        viewModel.prepareProjectSave()
        assertEquals(mapOf(0 to eye, 1 to hand), viewModel.state.value.packTags)
        assertEquals(2, viewModel.state.value.animation.totalFrames)
    }

    @Test
    fun `actor placement ordering stays a scene-only FxF operation`() {
        val first =
            ActorPlacementReference(
                "placement-first",
                "performance-1",
                "actor-1",
                "revision-1",
                "Alice walk",
                0,
                12,
                zIndex = 3,
            )
        val second =
            ActorPlacementReference(
                "placement-second",
                "performance-2",
                "actor-2",
                "revision-2",
                "Bob wave",
                0,
                12,
                zIndex = 7,
            )
        viewModel.handleIntent(StudioIntent.ActorPlacement.Add(first))
        viewModel.handleIntent(StudioIntent.ActorPlacement.Add(second))

        viewModel.handleIntent(StudioIntent.ActorPlacement.BringToFront("placement-first"))
        assertEquals(8, viewModel.state.value.project.actorPlacements.getValue("placement-first").zIndex)
        viewModel.handleIntent(StudioIntent.ActorPlacement.SendToBack("placement-second"))
        assertEquals(6, viewModel.state.value.project.actorPlacements.getValue("placement-second").zIndex)
    }

    @Test
    fun `puppet workspace freezes playback and rejects raster tool changes`() {
        viewModel.handleIntent(StudioIntent.Playback.Play)
        viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
        assertFalse(viewModel.state.value.animation.isPlaying)
        val before = viewModel.state.value.animation
        viewModel.handleIntent(StudioIntent.Playback.Play)
        viewModel.handleIntent(StudioIntent.Playback.SetFps(24))
        viewModel.handleIntent(StudioIntent.Playback.NextFrame)
        viewModel.handleIntent(StudioIntent.Draw.SelectTool(DrawingTool.BRUSH))
        assertEquals(before, viewModel.state.value.animation)
        assertEquals(StudioMode.RIG_SETUP, viewModel.state.value.scene.studioMode)
        assertEquals(DrawingTool.RIG_SELECT, viewModel.state.value.tool.activeTool)
        viewModel.handleIntent(
            StudioIntent.Actor.Create(com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN),
        )
        viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING))
        assertEquals(DrawingTool.BRUSH, viewModel.state.value.tool.activeTool)
        val restored = viewModel.state.value
        assertEquals("main", restored.currentFrameLayers()[restored.layers.activeLayerIndex].id)
        assertFalse(restored.actor.canUndo)
        assertFalse(viewModel.state.value.animation.isPlaying)
    }

    @Test
    fun `Play intent updates state and increments frame`() = runTest {
        viewModel.handleIntent(StudioIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(StudioIntent.Playback.Play)
        assertTrue(viewModel.state.value.animation.isPlaying)

        // Advance time to trigger frame ticks
        advanceTimeBy(100)

        assertTrue(viewModel.state.value.animation.currentFrame in 0..1)

        viewModel.handleIntent(StudioIntent.Playback.Pause)
    }

    @Test
    fun `Pause intent stops animation`() = runTest {
        viewModel.handleIntent(StudioIntent.Playback.Play)
        assertTrue(viewModel.state.value.animation.isPlaying)

        viewModel.handleIntent(StudioIntent.Playback.Pause)
        assertFalse(viewModel.state.value.animation.isPlaying)

        val frameBeforeWait = viewModel.state.value.animation.currentFrame
        advanceTimeBy(100)
        assertEquals(frameBeforeWait, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `NextFrame intent increments frame`() {
        viewModel.handleIntent(StudioIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(StudioIntent.Playback.NextFrame)
        assertEquals(0, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `PreviousFrame intent wraps to totalFrames minus one at start`() {
        viewModel.handleIntent(StudioIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(StudioIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(StudioIntent.Playback.SetCurrentFrame(2))
        assertEquals(2, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(StudioIntent.Playback.PreviousFrame)
        assertEquals(1, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(StudioIntent.Playback.PreviousFrame)
        assertEquals(0, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(StudioIntent.Playback.PreviousFrame)
        assertEquals(2, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `SelectTool updates activeTool`() {
        viewModel.handleIntent(StudioIntent.Draw.SelectTool(DrawingTool.ERASER))
        assertEquals(DrawingTool.ERASER, viewModel.state.value.tool.activeTool)

        viewModel.handleIntent(StudioIntent.Draw.SelectTool(DrawingTool.BRUSH))
        assertEquals(DrawingTool.BRUSH, viewModel.state.value.tool.activeTool)
    }

    @Test
    fun `UpdateTransform updates pan and zoom`() {
        val newPan = Offset(100f, 200f)
        val newZoom = 2.5f
        viewModel.handleIntent(StudioIntent.View.UpdateTransform(newPan, newZoom, 0f, Offset.Zero))
        assertEquals(newPan, viewModel.state.value.canvas.pan)
        assertEquals(newZoom, viewModel.state.value.canvas.zoom)
    }

    @Test
    fun `AddFrame intent increments totalFrames and updates currentFrame`() = runTest {
        val initialTotal = viewModel.state.value.animation.totalFrames
        val initialCurrent = viewModel.state.value.animation.currentFrame

        viewModel.handleIntent(StudioIntent.Frame.AddFrame)

        // Advance time to allow the coroutine in addFrame to finish
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialTotal + 1, viewModel.state.value.animation.totalFrames)
        assertEquals(initialCurrent + 1, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `AddText intent creates a TextNode in SceneState`() = runTest {
        val text = "Hello World"
        viewModel.handleIntent(StudioIntent.Text.AddText(text, 100f, 200f))

        val nodes = viewModel.state.value.scene.nodes
        assertEquals(1, nodes.size)
        val node = nodes.values.first() as com.donnclab.kinetiframe.core.document.TextNode
        assertEquals(text, node.text)
        assertEquals(100f, node.x)
        assertEquals(200f, node.y)
    }

    @Test
    fun `ToggleSkeletonOverlay flips visibility without touching document state`() = runTest {
        assertTrue("overlay defaults to visible", viewModel.state.value.scene.skeletonOverlayVisible)
        val projectBefore = viewModel.state.value.project

        viewModel.handleIntent(StudioIntent.Rigging.ToggleSkeletonOverlay)
        assertFalse(viewModel.state.value.scene.skeletonOverlayVisible)
        assertEquals(
            "hiding the overlay is a view preference, not a rig edit",
            projectBefore,
            viewModel.state.value.project,
        )

        viewModel.handleIntent(StudioIntent.Rigging.ToggleSkeletonOverlay)
        assertTrue(viewModel.state.value.scene.skeletonOverlayVisible)
    }

    @Test
    fun `ToggleGroup on the Actors track collapses and re-expands it, independent of other groups`() = runTest {
        assertTrue("no group starts collapsed", viewModel.state.value.tracks.collapsedGroups.isEmpty())

        viewModel.handleIntent(StudioIntent.Tracks.ToggleGroup(TrackGroupKind.ACTORS))
        assertEquals(setOf(TrackGroupKind.ACTORS), viewModel.state.value.tracks.collapsedGroups)

        viewModel.handleIntent(StudioIntent.Tracks.ToggleGroup(TrackGroupKind.ACTORS))
        assertTrue(viewModel.state.value.tracks.collapsedGroups.isEmpty())
    }
}
