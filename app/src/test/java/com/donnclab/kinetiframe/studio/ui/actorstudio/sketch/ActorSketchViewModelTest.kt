package com.donnclab.kinetiframe.studio.ui.actorstudio.sketch

import android.graphics.Color
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPoint
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchEffect
import com.donnclab.kinetiframe.presentation.actors.sketch.ActorSketchIntent
import com.donnclab.kinetiframe.presentation.actors.sketch.SketchTool
import com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch.ActorSketchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActorSketchViewModelTest {
    @Test fun `binding publishes original layers and cohesive placement with independent source IDs`() = runTest(
        dispatcher,
    ) {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 64f)))
        val model = ActorSketchViewModel(graph, 32, 16, floatArrayOf(2f, 3f, 64f, 32f), null, null, dispatcher)
        runCurrent()
        model.handleIntent(ActorSketchIntent.Colour(Color.CYAN))
        model.handleIntent(ActorSketchIntent.Tool(SketchTool.FILL))
        model.handleIntent(ActorSketchIntent.Fill(16, 8))
        runCurrent()
        model.handleIntent(ActorSketchIntent.Bind)
        runCurrent()
        val published = requireNotNull(model.effect.first() as? ActorSketchEffect.Bound)
        try {
            assertEquals(SkinSourceSize(32, 16), published.drawing.source.size)
            assertEquals(DrawingPlacement(Point2(2f, 3f), DrawingExtent(64f, 32f)), published.drawing.placement)
            assertNotEquals(published.drawing.inkAssetId, published.drawing.colourAssetId)
            assertEquals(Color.CYAN, published.colour.getPixel(16, 8))
            assertEquals(0, published.ink.getPixel(16, 8))
            assertNotSame(model.state.value.ink, published.ink)
            assertNotSame(model.state.value.colour, published.colour)
            ActorGraphValidation.validate(published.restGraph.copy(drawing = published.drawing))
            assertFalse(model.state.value.busy)
        } finally {
            published.ink.recycle()
            published.colour.recycle()
            androidx.lifecycle.ViewModelStore().apply {
                put("sketch", model)
                clear()
            }
        }
    }

    @Test fun `joint fitting changes draft only and undo restores skeleton with pixels untouched`() = runTest(
        dispatcher,
    ) {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 32f)))
        val model = ActorSketchViewModel(graph, 32, 32, floatArrayOf(0f, 0f, 32f, 32f), null, null, dispatcher)
        runCurrent()
        val ink = model.state.value.ink
        model.handleIntent(ActorSketchIntent.MoveJoint("root", false, androidx.compose.ui.geometry.Offset(16f, 16f)))
        assertNotEquals(graph, model.state.value.graph)
        assertEquals(32f, graph.bones.getValue("root").length)
        assertSame(ink, model.state.value.ink)
        model.handleIntent(ActorSketchIntent.Undo)
        assertEquals(graph, model.state.value.graph)
        model.handleIntent(ActorSketchIntent.Redo)
        assertNotEquals(graph, model.state.value.graph)
    }
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun `erase clears ink rather than painting black and undo restores pixels`() = runTest(dispatcher) {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 32f)))
        val model = ActorSketchViewModel(graph, 32, 32, floatArrayOf(0f, 0f, 32f, 32f), null, null, dispatcher)
        runCurrent()
        val line = listOf(TouchPoint(4f, 16f), TouchPoint(28f, 16f))
        model.handleIntent(ActorSketchIntent.Size(6f))
        model.handleIntent(ActorSketchIntent.Stroke(line))
        runCurrent()
        assertEquals(Color.BLACK, model.state.value.ink!!.getPixel(16, 16))
        model.handleIntent(ActorSketchIntent.Tool(SketchTool.ERASER))
        model.handleIntent(ActorSketchIntent.Stroke(line))
        runCurrent()
        assertEquals(0, model.state.value.ink!!.getPixel(16, 16))
        model.handleIntent(ActorSketchIntent.Undo)
        assertEquals(Color.BLACK, model.state.value.ink!!.getPixel(16, 16))
        model.handleIntent(ActorSketchIntent.Redo)
        assertEquals(0, model.state.value.ink!!.getPixel(16, 16))
    }

    @Test fun `colour edits do not bake over linework`() = runTest(dispatcher) {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 32f)))
        val model = ActorSketchViewModel(graph, 32, 32, floatArrayOf(0f, 0f, 32f, 32f), null, null, dispatcher)
        runCurrent()
        model.handleIntent(ActorSketchIntent.Colour(Color.CYAN))
        model.handleIntent(ActorSketchIntent.Tool(SketchTool.FILL))
        model.handleIntent(ActorSketchIntent.Fill(16, 16))
        runCurrent()
        assertEquals(Color.CYAN, model.state.value.colour!!.getPixel(16, 16))
        assertEquals(0, model.state.value.ink!!.getPixel(16, 16))
        model.handleIntent(ActorSketchIntent.Undo)
        assertEquals(0, model.state.value.colour!!.getPixel(16, 16))
    }

    @Test fun `binding lassos undo without changing either source bitmap`() = runTest(dispatcher) {
        val graph = SceneGraph(listOf("root"), mapOf("root" to BoneNode("root", name = "Root", length = 32f)))
        val model = ActorSketchViewModel(graph, 32, 32, floatArrayOf(0f, 0f, 32f, 32f), null, null, dispatcher)
        runCurrent()
        val ink = model.state.value.ink
        model.handleIntent(
            ActorSketchIntent.BindRegion(
                listOf(DrawingPoint(.1f, .1f), DrawingPoint(.9f, .1f), DrawingPoint(.5f, .9f)),
            ),
        )
        assertEquals(1, model.state.value.regions.size)
        assertSame(ink, model.state.value.ink)
        model.handleIntent(ActorSketchIntent.Undo)
        assertTrue(model.state.value.regions.isEmpty())
        model.handleIntent(ActorSketchIntent.Redo)
        assertEquals(1, model.state.value.regions.size)
        model.handleIntent(ActorSketchIntent.ClearRegions("root"))
        assertTrue(model.state.value.regions.isEmpty())
    }
}
