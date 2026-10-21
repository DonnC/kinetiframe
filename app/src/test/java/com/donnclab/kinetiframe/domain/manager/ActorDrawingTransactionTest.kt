package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.ActorDrawingBinder
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawing
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.DrawingBinding
import com.donnclab.kinetiframe.engine.rigging.model.DrawingExtent
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPlacement
import com.donnclab.kinetiframe.engine.rigging.model.DrawingSource
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.actorDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ActorDrawingTransactionTest {
    @Test fun `source and fitted joints apply atomically and undo together`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val definition = state.actorDefinition()!!
            val rest = com.donnclab.kinetiframe.engine.rigging.math.ActorPoseEditor.moveOrigin(
                definition.sceneGraph,
                "root",
                androidx.compose.ui.geometry.Offset(20f, 30f),
            )
            val drawing = ActorDrawing(
                DrawingSource("draft-ink", "draft-colour", SkinSourceSize(16, 16)),
                DrawingPlacement(Point2(0f, 0f), DrawingExtent(16f, 16f)),
                DrawingBinding(
                    ActorDrawingBinder.bind(
                        rest,
                        SkinSourceSize(16, 16),
                        DrawingPlacement(Point2(0f, 0f), DrawingExtent(16f, 16f)),
                    ),
                ),
            )
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            manager.commitDrawing(
                TimelineIntent.Actor.CommitDrawing(
                    definition.id,
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    drawing,
                    rest,
                ),
            )
            assertEquals(rest.bones, state.actorDefinition()!!.sceneGraph.bones)
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(definition.sceneGraph, state.actorDefinition()!!.sceneGraph)
        } finally {
            runtime.clear()
        }
    }

    @Test fun `whole drawing commits as one transaction and undo preserves both sources`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val definition = state.actorDefinition()!!
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val drawing = ActorDrawing(
                DrawingSource("ink", "colour", SkinSourceSize(16, 16)),
                DrawingPlacement(Point2(0f, 0f), DrawingExtent(16f, 16f)),
                DrawingBinding(
                    ActorDrawingBinder.bind(
                        definition.sceneGraph,
                        SkinSourceSize(16, 16),
                        DrawingPlacement(Point2(0f, 0f), DrawingExtent(16f, 16f)),
                    ),
                ),
            )
            manager.commitDrawing(
                TimelineIntent.Actor.CommitDrawing(
                    definition.id,
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    drawing,
                ),
            )
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(TimelineIntent.Actor.Undo)
            assertNull(state.actorDefinition()!!.sceneGraph.drawing)
            assertFalse(runtime.getAsset("ink")!!.isRecycled)
            studio.handle(TimelineIntent.Actor.Redo)
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(TimelineIntent.Actor.Drag("origin:root", androidx.compose.ui.geometry.Offset(100f, 200f)))
            val preview = state.actor.previewGraph!!
            val points = com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinning.deform(
                preview.drawing!!.skin,
                com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinning.worldTransforms(preview),
            )
            drawing.skin.vertices.forEachIndexed { i, v ->
                assertEquals(v.x, points[i * 2], .001f)
                assertEquals(
                    v.y,
                    points[
                        i *
                            2 +
                            1,
                    ],
                    .001f,
                )
            }
            studio.handle(TimelineIntent.Actor.CommitDrag)
            assertEquals(drawing.inkAssetId, state.actorDefinition()!!.sceneGraph.drawing!!.inkAssetId)
            manager.setDrawingOverlay("root", true)
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts)
            manager.setDrawingOverlay("root", true, behind = true)
            assertTrue(state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts.isEmpty())
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.underlayParts)
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts)
            studio.handle(TimelineIntent.Actor.Undo)
            assertTrue(state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts.isEmpty())
            manager.useDrawing(false)
            assertFalse(state.actorDefinition()!!.sceneGraph.drawing!!.enabled)
            state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
            manager.useDrawing(true)
            assertFalse(state.actorDefinition()!!.sceneGraph.drawing!!.enabled)
        } finally {
            runtime.clear()
        }
    }
}
