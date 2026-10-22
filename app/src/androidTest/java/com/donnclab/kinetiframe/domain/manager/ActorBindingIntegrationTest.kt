package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.math.RigGeometry
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.render.PuppetRenderPlan
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioEffect
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Device graphics integration coverage. Pure actor math remains in the JVM suite;
 * this test deliberately exercises Android's actual Bitmap implementation.
 */
class ActorBindingIntegrationTest {
    @Test fun extractionBindsSelectedRoleWithoutMovingSourcePixelsAndCancelReleasesTemporaryAsset() = runBlocking {
        var state = StudioState(
            project = ProjectDocument(stage = StageDimensions(128, 128)),
            scene = SceneState(studioMode = StudioMode.RIG_SETUP),
        )
        val authoring = ActorStudioManager({ state }, { state = it(state) })
        authoring.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(selectedBoneId = "left_forearm"))
        val assets = RigAssetManager()
        val failures = mutableListOf<StudioEffect>()
        val manager =
            RiggingManager(this, assets, { state }, { state = it(state) }, failures::add, authoring::checkpoint)
        val source = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val path = Path().apply { addRect(Rect(10f, 10f, 50f, 50f)) }
        try {
            val rest = state.actorDefinition()!!
            manager.preparePivotPlacement(source, path, 0)
            withTimeout(10_000) { while (state.scene.pivotPlacement == null && failures.isEmpty()) delay(10) }
            assertTrue(failures.toString(), failures.isEmpty())
            manager.confirmPivot()
            val bound = state.actorDefinition()!!
            assertEquals(rest.sceneGraph.bones.size, bound.sceneGraph.bones.size)
            assertEquals(
                rest.sceneGraph.bones.getValue("left_forearm").localTransform,
                bound.sceneGraph.bones.getValue("left_forearm").localTransform,
            )
            assertNotNull(bound.sceneGraph.bones.getValue("left_forearm").textureBinding)
            val command = PuppetRenderPlan.create(bound, state.activeActor()!!).single()
            val origin = floatArrayOf(0f, 0f)
            command.matrix.mapPoints(origin)
            assertEquals(10f, origin[0], 0.01f)
            assertEquals(10f, origin[1], 0.01f)

            // The attachment survives a pose transaction, which remains instance-local.
            state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
            val endpoint = RigGeometry.resolve(bound.sceneGraph).getValue("left_forearm").end
            authoring.handle(StudioIntent.Actor.Drag("left_forearm", endpoint + Offset(20f, -25f)))
            authoring.handle(StudioIntent.Actor.CommitDrag)
            assertTrue(state.activeActor()!!.pose.isNotEmpty())
            authoring.handle(StudioIntent.Actor.Undo)
            assertTrue(state.activeActor()!!.pose.isEmpty())
            authoring.handle(StudioIntent.Actor.Redo)
            assertTrue(state.activeActor()!!.pose.isNotEmpty())
            authoring.handle(StudioIntent.Actor.Undo)

            state = state.copy(scene = state.scene.copy(studioMode = StudioMode.RIG_SETUP))
            manager.preparePivotPlacement(source, path, 0)
            withTimeout(10_000) { while (state.scene.pivotPlacement == null && failures.isEmpty()) delay(10) }
            assertEquals(2, assets.size())
            manager.cancelPivot()
            assertEquals(1, assets.size())
            assertEquals(1, state.project.assets.size)
            authoring.handle(StudioIntent.Actor.Undo)
            assertEquals(rest, state.actorDefinition())
        } finally {
            source.recycle()
            assets.clear()
        }
    }

    /**
     * Detaching or rebinding a texture must not recycle a bitmap an undo snapshot still needs. This exercises
     * the real Android Bitmap, not a mock, through the exact manager wiring StudioViewModel
     * uses (RiggingManager's rebind reconciles through ActorStudioManager's reachable set).
     */
    @Test fun rebindingATextureRetainsThePreviousBitmapUntilUndoHistoryNoLongerNeedsIt() = runBlocking {
        var state = StudioState(
            project = ProjectDocument(stage = StageDimensions(128, 128)),
            scene = SceneState(studioMode = StudioMode.RIG_SETUP),
        )
        val assets = RigAssetManager()
        val authoring = ActorStudioManager({ state }, { state = it(state) }, retainRuntimeAssets = assets::retainOnly)
        authoring.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(selectedBoneId = "left_forearm"))
        val failures = mutableListOf<StudioEffect>()
        val manager = RiggingManager(
            this,
            assets,
            { state },
            { state = it(state) },
            failures::add,
            authoring::checkpoint,
            authoring::reconcileRuntimeAssets,
        )
        val source = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val path = Path().apply { addRect(Rect(10f, 10f, 50f, 50f)) }
        try {
            manager.preparePivotPlacement(source, path, 0)
            withTimeout(10_000) { while (state.scene.pivotPlacement == null && failures.isEmpty()) delay(10) }
            assertTrue(failures.toString(), failures.isEmpty())
            manager.confirmPivot()
            val firstAssetId = state.actorDefinition()!!.sceneGraph.bones.getValue(
                "left_forearm",
            ).textureBinding!!.assetId
            val firstBitmap = assets.getAsset(firstAssetId)!!

            // Re-extract onto the same already-bound part: this is a rebind, not a fresh bind,
            // so the checkpoint taken just before it still references the bitmap being replaced.
            manager.preparePivotPlacement(source, path, 0)
            withTimeout(10_000) { while (state.scene.pivotPlacement == null && failures.isEmpty()) delay(10) }
            assertTrue(failures.toString(), failures.isEmpty())
            manager.confirmPivot()
            val secondAssetId = state.actorDefinition()!!.sceneGraph.bones.getValue(
                "left_forearm",
            ).textureBinding!!.assetId
            assertNotEquals(firstAssetId, secondAssetId)

            // A naive "release the instant the live document drops it" policy would have
            // recycled firstBitmap right here, even though Undo is about to need it again.
            assertFalse("previous bitmap must survive while undo history still references it", firstBitmap.isRecycled)

            authoring.handle(StudioIntent.Actor.Undo)
            assertEquals(
                firstAssetId,
                state.actorDefinition()!!.sceneGraph.bones.getValue("left_forearm").textureBinding!!.assetId,
            )
            assertFalse(
                "Undo must be able to render the bitmap it just restored a reference to",
                firstBitmap.isRecycled,
            )
            assertSame(firstBitmap, assets.getAsset(firstAssetId))

            // Move forward again so the live document no longer references the first bitmap,
            // then drop history the way switching workspaces does; only now is it unreachable.
            authoring.handle(StudioIntent.Actor.Redo)
            authoring.clearHistory()
            assertTrue("once no snapshot or live binding needs it, the sweep may recycle it", firstBitmap.isRecycled)
        } finally {
            source.recycle()
            assets.clear()
        }
    }
}
