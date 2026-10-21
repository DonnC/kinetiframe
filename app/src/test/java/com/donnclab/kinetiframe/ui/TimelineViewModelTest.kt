package com.donnclab.kinetiframe.ui

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.DrawingTool
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
class TimelineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TimelineViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Use a temporary directory for the cache manager in tests
        val tempDir = java.io.File.createTempFile("kinetiframe_test_cache", "")
        tempDir.delete()
        tempDir.mkdir()
        viewModel = TimelineViewModel(tempDir, testDispatcher, shouldInitialize = false)
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
        val head = com.donnclab.kinetiframe.ui.packs.PackFrameTag("Head")
        val hand = com.donnclab.kinetiframe.ui.packs.PackFrameTag("Hand")
        val eye = com.donnclab.kinetiframe.ui.packs.PackFrameTag("Eye")
        viewModel.handleIntent(TimelineIntent.PackItem.SetTag(0, head))
        viewModel.handleIntent(TimelineIntent.PackItem.Add(1, hand))
        viewModel.prepareProjectSave()
        assertEquals(mapOf(0 to head, 1 to hand), viewModel.state.value.packTags)
        viewModel.handleIntent(TimelineIntent.PackItem.Add(0, eye))
        viewModel.prepareProjectSave()
        assertEquals(mapOf(0 to eye, 1 to head, 2 to hand), viewModel.state.value.packTags)
        viewModel.handleIntent(TimelineIntent.Frame.DeleteFrame(1))
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
        viewModel.handleIntent(TimelineIntent.ActorPlacement.Add(first))
        viewModel.handleIntent(TimelineIntent.ActorPlacement.Add(second))

        viewModel.handleIntent(TimelineIntent.ActorPlacement.BringToFront("placement-first"))
        assertEquals(8, viewModel.state.value.project.actorPlacements.getValue("placement-first").zIndex)
        viewModel.handleIntent(TimelineIntent.ActorPlacement.SendToBack("placement-second"))
        assertEquals(6, viewModel.state.value.project.actorPlacements.getValue("placement-second").zIndex)
    }

    @Test
    fun `puppet workspace freezes playback and rejects raster tool changes`() {
        viewModel.handleIntent(TimelineIntent.Playback.Play)
        viewModel.handleIntent(TimelineIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
        assertFalse(viewModel.state.value.animation.isPlaying)
        val before = viewModel.state.value.animation
        viewModel.handleIntent(TimelineIntent.Playback.Play)
        viewModel.handleIntent(TimelineIntent.Playback.SetFps(24))
        viewModel.handleIntent(TimelineIntent.Playback.NextFrame)
        viewModel.handleIntent(TimelineIntent.Draw.SelectTool(DrawingTool.BRUSH))
        assertEquals(before, viewModel.state.value.animation)
        assertEquals(StudioMode.RIG_SETUP, viewModel.state.value.scene.studioMode)
        assertEquals(DrawingTool.RIG_SELECT, viewModel.state.value.tool.activeTool)
        viewModel.handleIntent(
            TimelineIntent.Actor.Create(com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate.HUMAN),
        )
        viewModel.handleIntent(TimelineIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING))
        assertEquals(DrawingTool.BRUSH, viewModel.state.value.tool.activeTool)
        val restored = viewModel.state.value
        assertEquals("main", restored.currentFrameLayers()[restored.layers.activeLayerIndex].id)
        assertFalse(restored.actor.canUndo)
        assertFalse(viewModel.state.value.animation.isPlaying)
    }

    @Test
    fun `Play intent updates state and increments frame`() = runTest {
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(TimelineIntent.Playback.Play)
        assertTrue(viewModel.state.value.animation.isPlaying)

        // Advance time to trigger frame ticks
        advanceTimeBy(100)

        assertTrue(viewModel.state.value.animation.currentFrame in 0..1)

        viewModel.handleIntent(TimelineIntent.Playback.Pause)
    }

    @Test
    fun `Pause intent stops animation`() = runTest {
        viewModel.handleIntent(TimelineIntent.Playback.Play)
        assertTrue(viewModel.state.value.animation.isPlaying)

        viewModel.handleIntent(TimelineIntent.Playback.Pause)
        assertFalse(viewModel.state.value.animation.isPlaying)

        val frameBeforeWait = viewModel.state.value.animation.currentFrame
        advanceTimeBy(100)
        assertEquals(frameBeforeWait, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `NextFrame intent increments frame`() {
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(TimelineIntent.Playback.NextFrame)
        assertEquals(0, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `PreviousFrame intent wraps to totalFrames minus one at start`() {
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.handleIntent(TimelineIntent.Playback.SetCurrentFrame(2))
        assertEquals(2, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(TimelineIntent.Playback.PreviousFrame)
        assertEquals(1, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(TimelineIntent.Playback.PreviousFrame)
        assertEquals(0, viewModel.state.value.animation.currentFrame)

        viewModel.handleIntent(TimelineIntent.Playback.PreviousFrame)
        assertEquals(2, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `SelectTool updates activeTool`() {
        viewModel.handleIntent(TimelineIntent.Draw.SelectTool(DrawingTool.ERASER))
        assertEquals(DrawingTool.ERASER, viewModel.state.value.tool.activeTool)

        viewModel.handleIntent(TimelineIntent.Draw.SelectTool(DrawingTool.BRUSH))
        assertEquals(DrawingTool.BRUSH, viewModel.state.value.tool.activeTool)
    }

    @Test
    fun `UpdateTransform updates pan and zoom`() {
        val newPan = Offset(100f, 200f)
        val newZoom = 2.5f
        viewModel.handleIntent(TimelineIntent.View.UpdateTransform(newPan, newZoom, 0f, Offset.Zero))
        assertEquals(newPan, viewModel.state.value.canvas.pan)
        assertEquals(newZoom, viewModel.state.value.canvas.zoom)
    }

    @Test
    fun `AddFrame intent increments totalFrames and updates currentFrame`() = runTest {
        val initialTotal = viewModel.state.value.animation.totalFrames
        val initialCurrent = viewModel.state.value.animation.currentFrame

        viewModel.handleIntent(TimelineIntent.Frame.AddFrame)

        // Advance time to allow the coroutine in addFrame to finish
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialTotal + 1, viewModel.state.value.animation.totalFrames)
        assertEquals(initialCurrent + 1, viewModel.state.value.animation.currentFrame)
    }

    @Test
    fun `AddText intent creates a TextNode in SceneState`() = runTest {
        val text = "Hello World"
        viewModel.handleIntent(TimelineIntent.Text.AddText(text, 100f, 200f))

        val nodes = viewModel.state.value.scene.nodes
        assertEquals(1, nodes.size)
        val node = nodes.values.first() as com.donnclab.kinetiframe.models.TextNode
        assertEquals(text, node.text)
        assertEquals(100f, node.x)
        assertEquals(200f, node.y)
    }

    @Test
    fun `ToggleSkeletonOverlay flips visibility without touching document state`() = runTest {
        assertTrue("overlay defaults to visible", viewModel.state.value.scene.skeletonOverlayVisible)
        val projectBefore = viewModel.state.value.project

        viewModel.handleIntent(TimelineIntent.Rigging.ToggleSkeletonOverlay)
        assertFalse(viewModel.state.value.scene.skeletonOverlayVisible)
        assertEquals(
            "hiding the overlay is a view preference, not a rig edit",
            projectBefore,
            viewModel.state.value.project,
        )

        viewModel.handleIntent(TimelineIntent.Rigging.ToggleSkeletonOverlay)
        assertTrue(viewModel.state.value.scene.skeletonOverlayVisible)
    }

    @Test
    fun `ToggleGroup on the Actors track collapses and re-expands it, independent of other groups`() = runTest {
        assertTrue("no group starts collapsed", viewModel.state.value.tracks.collapsedGroups.isEmpty())

        viewModel.handleIntent(TimelineIntent.Tracks.ToggleGroup(TrackGroupKind.ACTORS))
        assertEquals(setOf(TrackGroupKind.ACTORS), viewModel.state.value.tracks.collapsedGroups)

        viewModel.handleIntent(TimelineIntent.Tracks.ToggleGroup(TrackGroupKind.ACTORS))
        assertTrue(viewModel.state.value.tracks.collapsedGroups.isEmpty())
    }
}
