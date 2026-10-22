package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.domain.render.ActorSelectionGeometry
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.ui.StudioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ActorSelectionTest {
    private val actor = ActorPlacementReference("placed", "performance", "actor", "revision", "Wave", 0, 24)
    private val quad = listOf(Offset(10f, 10f), Offset(30f, 10f), Offset(30f, 30f), Offset(10f, 30f))

    @Test fun rectangleSelectsLiveActorAndCommitsExactShear() = runTest {
        var state =
            StudioState(
                project = ProjectDocument(id = "project", actorPlacements = mapOf(actor.placementId to actor)),
            )
        val history = mutableListOf<HistoryItem>()
        val manager =
            SelectionManager(this, Dispatchers.Unconfined, Mutex(), {
                state
            }, { state = it(state) }, {}, { null }, {}, history)
        manager.selectArea(
            null,
            Rect(0f, 0f, 40f, 40f),
            1f,
            actors = ActorSelectionGeometry("project", 0, listOf(ActorSelectionGeometry.Object(actor, listOf(quad)))),
        ).join()
        assertEquals(setOf("placed"), state.selection.selectedContent!!.actors.keys)
        assertNull(state.selection.selectedContent!!.bitmap)
        state =
            state.copy(
                selection = state.selection.copy(
                    transform = Matrix().apply {
                        values[4] = .5f
                        values[12] = 20f
                    },
                ),
            )
        manager.commitSelection().join()
        val placed = state.project.actorPlacements.getValue("placed")
        assertEquals(.5f, placed.selectionTransform.c, 0f)
        assertEquals(20f, placed.selectionTransform.tx, 0f)
        assertEquals(actor.performanceId, placed.performanceId)
        assertEquals(actor.actorRevisionId, placed.actorRevisionId)
        assertTrue(history.single() is HistoryItem.ObjectEdit)
    }

    @Test fun transformedCopyPastesOnceAndCancelRemovesOnlyTheCopy() = runTest {
        var state =
            StudioState(
                project = ProjectDocument(id = "project", actorPlacements = mapOf(actor.placementId to actor)),
            )
        val manager =
            SelectionManager(this, Dispatchers.Unconfined, Mutex(), {
                state
            }, { state = it(state) }, {}, { null }, {}, mutableListOf())
        manager.selectArea(
            null,
            Rect(0f, 0f, 40f, 40f),
            1f,
            actors = ActorSelectionGeometry("project", 0, listOf(ActorSelectionGeometry.Object(actor, listOf(quad)))),
        ).join()
        state = state.copy(selection = state.selection.copy(transform = Matrix().apply { values[12] = 50f }))
        manager.copySelection()
        manager.pasteSelection()
        assertEquals(2, state.project.actorPlacements.size)
        assertEquals(70f, state.selection.transform.values[12], 0f)
        manager.cancelSelection().join()
        assertEquals(setOf("placed"), state.project.actorPlacements.keys)
        assertEquals(50f, state.project.actorPlacements.getValue("placed").selectionTransform.tx, 0f)
    }

    @Test fun staleFrameGeometryCannotSelectAnActor() = runTest {
        var state =
            StudioState(
                project = ProjectDocument(id = "project", actorPlacements = mapOf(actor.placementId to actor)),
            )
        val manager =
            SelectionManager(this, Dispatchers.Unconfined, Mutex(), {
                state
            }, { state = it(state) }, {}, { null }, {}, mutableListOf())
        manager.selectArea(
            null,
            Rect(0f, 0f, 40f, 40f),
            1f,
            actors = ActorSelectionGeometry("project", 9, listOf(ActorSelectionGeometry.Object(actor, listOf(quad)))),
        ).join()
        assertFalse(state.selection.active)
    }

    @Test fun maskFindsIntersectionEvenWhenNoObjectCornerIsInside() {
        val area = requireNotNull(SelectionAreaMask(null, Rect(15f, 15f, 25f, 25f)))
        assertTrue(area.matches(listOf(quad), partial = true))
        assertFalse(area.matches(listOf(quad), partial = false))
    }

    @Test fun pixelSelectionCancelDoesNotMutateBaseAndMoveIsOneHistoryStep() = runTest {
        var state = StudioState(project = ProjectDocument(stage = StageDimensions(32, 32)))
        val history = mutableListOf<HistoryItem>()
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
        val manager = SelectionManager(
            this, Dispatchers.Unconfined, Mutex(), { state }, { state = it(state) }, {},
            { source.copy(Bitmap.Config.ARGB_8888, true) }, { fail("Selection must not rewrite the base") }, history,
        )
        manager.selectArea(null, Rect(1f, 1f, 8f, 8f), 1f).join()
        manager.cancelSelection().join()
        assertTrue(history.isEmpty())
        assertEquals(android.graphics.Color.RED, source.getPixel(2, 2))
        manager.selectArea(null, Rect(1f, 1f, 8f, 8f), 1f).join()
        manager.commitSelection().join()
        assertTrue(history.single() is HistoryItem.RasterSelection)
        source.recycle()
    }
}
