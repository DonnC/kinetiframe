package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.actors.rig.math.ActorDrawingBinder
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.DrawingBinding
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingSource
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorArtworkManager
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorStudioManager
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
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(StudioIntent.Actor.Create(ActorTemplate.CUSTOM))
            val definition = state.actorDefinition()!!
            val rest = com.donnclab.kinetiframe.actors.rig.math.ActorPoseEditor.moveOrigin(
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
                StudioIntent.Actor.CommitDrawing(
                    definition.id,
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    drawing,
                    rest,
                ),
            )
            assertEquals(rest.bones, state.actorDefinition()!!.sceneGraph.bones)
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(StudioIntent.Actor.Undo)
            assertEquals(definition.sceneGraph, state.actorDefinition()!!.sceneGraph)
        } finally {
            runtime.clear()
        }
    }

    @Test fun `whole drawing commits as one transaction and undo preserves both sources`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(StudioIntent.Actor.Create(ActorTemplate.CUSTOM))
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
                StudioIntent.Actor.CommitDrawing(
                    definition.id,
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
                    drawing,
                ),
            )
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(StudioIntent.Actor.Undo)
            assertNull(state.actorDefinition()!!.sceneGraph.drawing)
            assertFalse(runtime.getAsset("ink")!!.isRecycled)
            studio.handle(StudioIntent.Actor.Redo)
            assertEquals(drawing, state.actorDefinition()!!.sceneGraph.drawing)
            studio.handle(StudioIntent.Actor.Drag("origin:root", androidx.compose.ui.geometry.Offset(100f, 200f)))
            val preview = state.actor.previewGraph!!
            val points = com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinning.deform(
                preview.drawing!!.skin,
                com.donnclab.kinetiframe.actors.rig.math.ArtworkSkinning.worldTransforms(preview),
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
            studio.handle(StudioIntent.Actor.CommitDrag)
            assertEquals(drawing.inkAssetId, state.actorDefinition()!!.sceneGraph.drawing!!.inkAssetId)
            manager.setDrawingOverlay("root", true)
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts)
            manager.setDrawingOverlay("root", true, behind = true)
            assertTrue(state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts.isEmpty())
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.underlayParts)
            studio.handle(StudioIntent.Actor.Undo)
            assertEquals(setOf("root"), state.actorDefinition()!!.sceneGraph.drawing!!.overlayParts)
            studio.handle(StudioIntent.Actor.Undo)
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
