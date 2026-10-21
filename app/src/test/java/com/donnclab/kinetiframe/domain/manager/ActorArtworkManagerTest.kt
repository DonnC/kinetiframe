package com.donnclab.kinetiframe.domain.manager

import android.graphics.Bitmap
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ArtworkAlignment
import com.donnclab.kinetiframe.engine.rigging.model.BoneNode
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.engine.rigging.model.Transform
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ActorArtworkManagerTest {
    @Test fun `attachment blend is reversible and performance editing cannot change its bind`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val definition = state.actorDefinition()!!
            val root = definition.sceneGraph.bones.getValue("root")
            val child = BoneNode(
                hierarchy = BoneHierarchy("child", "Attachment", "root"),
                rest = BoneRest(60f, localTransform = Transform(x = root.length)),
            )
            state = state.copy(
                project = state.project.copy(
                    puppetDefinitions = state.project.puppetDefinitions +
                        (
                            definition.id to
                                definition.copy(
                                    sceneGraph = definition.sceneGraph.copy(
                                        bones =
                                        definition.sceneGraph.bones + ("child" to child),
                                    ),
                                )
                            ),
                ),
            )
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val pixels = Bitmap.createBitmap(100, 30, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork("child", "Part", pixels, TextureBinding("pending", 20f, 15f), false),
            )
            val original = state.actorDefinition()!!.sceneGraph.bones.getValue("child")
            manager.blendAttachment(TimelineIntent.Actor.SetAttachmentBlend("child", enabled = true))
            val blended = state.actorDefinition()!!.sceneGraph.bones.getValue("child")
            assertTrue(blended.textureBinding!!.skin!!.attachmentBlend)
            assertEquals(original.localTransform, blended.localTransform)
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(original, state.actorDefinition()!!.sceneGraph.bones.getValue("child"))
            studio.handle(TimelineIntent.Actor.Redo)
            assertEquals(blended, state.actorDefinition()!!.sceneGraph.bones.getValue("child"))
            state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
            manager.blendAttachment(TimelineIntent.Actor.SetAttachmentBlend("child", enabled = false))
            assertEquals(blended, state.actorDefinition()!!.sceneGraph.bones.getValue("child"))
            state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = false))
            manager.blendAttachment(TimelineIntent.Actor.SetAttachmentBlend("child", enabled = false))
            assertNull(state.actorDefinition()!!.sceneGraph.bones.getValue("child").textureBinding!!.skin)
            assertFalse(pixels.isRecycled)
        } finally {
            runtime.clear()
        }
    }

    @Test fun `resize and removal preserve joints and undo reachable pixels`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val pixels = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork("root", "Body", pixels, TextureBinding("pending", 6f, 4f), false),
            )
            val original = state.actorDefinition()!!.sceneGraph.bones.getValue("root")
            val binding = original.textureBinding!!
            manager.update(
                TimelineIntent.Actor.UpdateArtwork(
                    "root",
                    "",
                    "Body",
                    binding.copy(
                        alignment = binding.alignment.copy(scale = 2f, offset = binding.alignment.offset.copy(y = 4f)),
                    ),
                ),
            )
            val resized = state.actorDefinition()!!.sceneGraph.bones.getValue("root")
            assertEquals(original.localTransform, resized.localTransform)
            assertEquals(2f, resized.textureBinding!!.scale)
            manager.remove(TimelineIntent.Actor.RemoveArtwork("root"))
            assertNull(state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding)
            assertFalse(pixels.isRecycled)
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(resized, state.actorDefinition()!!.sceneGraph.bones.getValue("root"))
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(original, state.actorDefinition()!!.sceneGraph.bones.getValue("root"))
            state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
            manager.remove(TimelineIntent.Actor.RemoveArtwork("root"))
            assertEquals(original, state.actorDefinition()!!.sceneGraph.bones.getValue("root"))
        } finally {
            runtime.clear()
        }
    }

    @Test fun `alternate replacement keeps its address and removal cleans only its timed keys`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            fun pixels() = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork("root", "Open", pixels(), TextureBinding("pending", 0f, 0f), true),
            )
            val id = state.actorDefinition()!!.sceneGraph.bones.getValue("root").artworkVariants.keys.single()
            manager.attach(
                TimelineIntent.Actor.AttachArtwork(
                    "root",
                    "New open",
                    pixels(),
                    TextureBinding("pending", 0f, 0f),
                    true,
                    replaceVariantId = id,
                ),
            )
            assertEquals(setOf(id), state.actorDefinition()!!.sceneGraph.bones.getValue("root").artworkVariants.keys)
            state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
            manager.select("root", id)
            state = state.copy(scene = state.scene.copy(studioMode = StudioMode.RIG_SETUP))
            manager.remove(TimelineIntent.Actor.RemoveArtwork("root", id))
            assertTrue(state.activeActor()!!.artworkKeys.isEmpty())
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(id, state.activeActor()!!.artworkKeys.single().selections["root"])
        } finally {
            runtime.clear()
        }
    }

    @Test fun `pack items become independent default and expression assets usable in performance`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val defaultPixels = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
            val expressionPixels = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)
            val alignment =
                TextureBinding(
                    "pending",
                    com.donnclab.kinetiframe.core.geometry.Point2(6f, 4f),
                    com.donnclab.kinetiframe.engine.rigging.model.ArtworkAlignment(
                        com.donnclab.kinetiframe.core.geometry.Point2(2f, 3f),
                        scale = 2f,
                    ),
                )
            manager.attach(TimelineIntent.Actor.AttachArtwork("root", "Neutral", defaultPixels, alignment, false))
            val original = state.actorDefinition()!!.sceneGraph.bones.getValue("root")
            manager.attach(TimelineIntent.Actor.AttachArtwork("root", "Talking", expressionPixels, alignment, true))
            val bone = state.actorDefinition()!!.sceneGraph.bones.getValue("root")
            assertEquals(original.textureBinding, bone.textureBinding)
            assertEquals(original.localTransform, bone.localTransform)
            val (variantId, variant) = bone.artworkVariants.entries.single()
            assertEquals("Talking", variant.name)
            assertNotEquals(bone.textureBinding!!.assetId, variant.binding.assetId)
            assertEquals(6f, variant.binding.pivotX)
            assertEquals(2f, variant.binding.scale)
            state = state.copy(
                scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
                actor = state.actor.copy(isPerformanceAuthoring = true, previewTimeMicros = 250_000),
            )
            manager.select("root", variantId)
            assertEquals(variantId, state.activeActor()!!.artworkKeys.single().selections["root"])
            assertEquals(bone, state.actorDefinition()!!.sceneGraph.bones.getValue("root"))
        } finally {
            runtime.clear()
        }
    }

    @Test fun `continuous limb attachment builds skin and undo restores original cutouts`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            val definition = state.actorDefinition()!!
            val root = definition.sceneGraph.bones.getValue("root")
            val child = BoneNode(
                hierarchy = BoneHierarchy("child", "Lower limb", "root"),
                rest = BoneRest(60f, localTransform = Transform(x = root.length)),
            )
            state = state.copy(
                project = state.project.copy(
                    puppetDefinitions = state.project.puppetDefinitions +
                        (
                            definition.id to
                                definition.copy(
                                    sceneGraph = definition.sceneGraph.copy(
                                        bones =
                                        definition.sceneGraph.bones + ("child" to child),
                                    ),
                                )
                            ),
                ),
            )
            studio.clearHistory()
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val pixels = Bitmap.createBitmap(100, 30, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork(
                    "root",
                    "Arm",
                    pixels,
                    TextureBinding("pending", 0f, 15f),
                    false,
                    "child",
                ),
            )
            val skin = state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding!!.skin!!
            assertEquals(listOf("root", "child"), skin.bones.map { it.boneId })
            studio.handle(TimelineIntent.Actor.Undo)
            assertNull(state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding)
            assertFalse(pixels.isRecycled)
            studio.handle(TimelineIntent.Actor.Redo)
            assertEquals(skin, state.actorDefinition()!!.sceneGraph.bones.getValue("root").textureBinding!!.skin)
        } finally {
            runtime.clear()
        }
    }

    @Test fun `attachment and timed selection undo independently without recycling history artwork`() {
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val runtime = RigAssetManager()
        try {
            val studio = ActorStudioManager({ state }, { state = it(state) }, retainRuntimeAssets = runtime::retainOnly)
            studio.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
            studio.clearHistory()
            val manager =
                ActorArtworkManager(runtime, {
                    state
                }, { state = it(state) }, studio::checkpoint, studio::reconcileRuntimeAssets, {})
            val pixels = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork("root", "Smile", pixels, TextureBinding("pending", 2f, 2f), true),
            )
            val variant = state.actorDefinition()!!.sceneGraph.bones.getValue("root").artworkVariants.keys.single()
            assertFalse(pixels.isRecycled)
            studio.handle(TimelineIntent.Actor.Undo)
            assertTrue(state.actorDefinition()!!.sceneGraph.bones.getValue("root").artworkVariants.isEmpty())
            assertFalse(pixels.isRecycled)
            studio.handle(TimelineIntent.Actor.Redo)
            state = state.copy(
                scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
                actor = state.actor.copy(isPerformanceAuthoring = true, previewTimeMicros = 500_000),
            )
            manager.select("root", variant)
            assertEquals(variant, state.activeActor()!!.artworkKeys.single().selections["root"])
            studio.handle(TimelineIntent.Actor.Undo)
            assertTrue(state.activeActor()!!.artworkKeys.isEmpty())
            assertEquals(1, state.actorDefinition()!!.sceneGraph.bones.getValue("root").artworkVariants.size)
            val rejected = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            manager.attach(
                TimelineIntent.Actor.AttachArtwork("root", "Wrong", rejected, TextureBinding("pending", 0f, 0f), true),
            )
            assertTrue(rejected.isRecycled)
            assertEquals(1, runtime.size())
        } finally {
            runtime.clear()
        }
    }
}
