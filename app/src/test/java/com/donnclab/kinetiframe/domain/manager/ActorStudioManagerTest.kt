package com.donnclab.kinetiframe.domain.manager

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.engine.model.TextureSlot
import com.donnclab.kinetiframe.engine.rigging.math.RigGeometry
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding
import com.donnclab.kinetiframe.ui.ActorEditTarget
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorStudioManagerTest {
    @Test fun `part display name changes without breaking semantic IDs and can undo`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
        val original = state.actorDefinition()!!.sceneGraph
        state = state.copy(scene = state.scene.copy(selectedBoneId = "root"))
        manager.handle(TimelineIntent.Actor.RenameBone("Flag base"))
        assertEquals("Flag base", state.actorDefinition()!!.sceneGraph.bones.getValue("root").name)
        assertEquals(original.bones.keys, state.actorDefinition()!!.sceneGraph.bones.keys)
        manager.handle(TimelineIntent.Actor.Undo)
        assertEquals(original, state.actorDefinition()!!.sceneGraph)
    }

    @Test fun `part stacking is undoable without modifying rig or performance`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        val original = state.actorDefinition()!!.sceneGraph
        val keys = state.activeActor()!!.poseKeys
        val part = com.donnclab.kinetiframe.engine.rigging.math.ActorPartStack.orderedIds(original).last()
        manager.handle(TimelineIntent.Actor.MovePart(part, false))
        val moved = state.actorDefinition()!!.sceneGraph
        assertNotEquals(original, moved)
        assertEquals(keys, state.activeActor()!!.poseKeys)
        manager.handle(TimelineIntent.Actor.Undo)
        assertEquals(original, state.actorDefinition()!!.sceneGraph)
        manager.handle(TimelineIntent.Actor.Redo)
        assertEquals(moved, state.actorDefinition()!!.sceneGraph)
    }

    @Test fun `performance cannot reorder its pinned actor`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        val original = state.project
        state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
        manager.handle(TimelineIntent.Actor.MovePart("torso", true))
        assertEquals(original, state.project)
    }
    private var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
    private val manager = ActorStudioManager({ state }, { state = it(state) })

    @Test fun `artwork dragging is cancellable undoable and cannot modify pinned performance`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.CUSTOM))
        val definition = state.actorDefinition()!!
        val root = definition.sceneGraph.bones.getValue(
            "root",
        ).withArtwork(textureBinding = TextureBinding("image", 0f, 0f))
        state = state.copy(
            project = state.project.copy(
                puppetDefinitions = state.project.puppetDefinitions +
                    (
                        definition.id to
                            definition.copy(sceneGraph = definition.sceneGraph.copy(bones = mapOf("root" to root)))
                        ),
            ),
        )
        val original = state.actorDefinition()!!
        manager.handle(TimelineIntent.Actor.SetEditTarget(ActorEditTarget.ARTWORK))
        manager.handle(TimelineIntent.Actor.Drag("artwork:root", Offset.Zero))
        manager.handle(TimelineIntent.Actor.Drag("artwork:root", Offset(10f, 20f)))
        manager.handle(TimelineIntent.Actor.CancelDrag)
        assertEquals(original, state.actorDefinition())
        manager.handle(TimelineIntent.Actor.Drag("artwork:root", Offset.Zero))
        manager.handle(TimelineIntent.Actor.Drag("artwork:root", Offset(10f, 20f)))
        manager.handle(TimelineIntent.Actor.CommitDrag)
        assertNotEquals(original, state.actorDefinition())
        assertEquals(root.localTransform, state.actorDefinition()!!.sceneGraph.bones.getValue("root").localTransform)
        manager.handle(TimelineIntent.Actor.Undo)
        assertEquals(original, state.actorDefinition())
        state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
        manager.handle(TimelineIntent.Actor.SetEditTarget(ActorEditTarget.JOINT_START))
        assertEquals(ActorEditTarget.ARTWORK, state.actor.editTarget)
    }

    @Test fun `create pose undo redo and reset leave blueprint unchanged`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        val rest = state.actorDefinition()!!
        state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
        val endpoint = RigGeometry.resolve(rest.sceneGraph).getValue("left_forearm").end
        manager.handle(TimelineIntent.Actor.Drag("left_forearm", endpoint + Offset(30f, -40f)))
        assertEquals(rest, state.actorDefinition())
        manager.handle(TimelineIntent.Actor.CommitDrag)
        val pose = state.activeActor()!!.pose
        assertTrue(pose.isNotEmpty())
        assertEquals(rest, state.actorDefinition())
        manager.handle(TimelineIntent.Actor.Undo)
        assertTrue(state.activeActor()!!.pose.isEmpty())
        manager.handle(TimelineIntent.Actor.Redo)
        assertEquals(pose, state.activeActor()!!.pose)
        manager.handle(TimelineIntent.Actor.ResetPose)
        assertTrue(state.activeActor()!!.pose.isEmpty())
    }

    @Test fun `cancelled drag has no document change or history entry`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.ANIMAL))
        val project = state.project
        manager.handle(TimelineIntent.Actor.Drag("head", Offset(1000f, 300f)))
        manager.handle(TimelineIntent.Actor.CancelDrag)
        assertEquals(project, state.project)
        assertNull(state.actor.previewGraph)
        manager.handle(TimelineIntent.Actor.Undo)
        assertTrue(state.project.puppetDefinitions.isEmpty())
    }

    /** Binds a texture directly onto the given bone, bypassing the lasso/pivot flow. */
    private fun bindTexture(boneId: String, assetId: String) {
        val definition = state.actorDefinition()!!
        val bone = definition.sceneGraph.bones.getValue(boneId)
        val slotId = "slot-$boneId"
        val bound = bone.withArtwork(textureSlotId = slotId, textureBinding = TextureBinding(assetId, 0f, 0f))
        state = state.copy(
            project = state.project.copy(
                puppetDefinitions = state.project.puppetDefinitions +
                    (
                        definition.id to definition.copy(
                            sceneGraph = definition.sceneGraph.copy(
                                bones =
                                definition.sceneGraph.bones + (boneId to bound),
                            ),
                            textureSlots = definition.textureSlots + (slotId to TextureSlot(slotId, assetId)),
                        )
                        ),
            ),
        )
    }

    @Test fun `detaching a bound texture keeps its asset retained while undo still needs it`() {
        var retained: Set<String> = emptySet()
        val trackingManager =
            ActorStudioManager({ state }, { state = it(state) }, retainRuntimeAssets = { retained = it })
        trackingManager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        bindTexture("torso", "tex-torso")
        state = state.copy(scene = state.scene.copy(selectedBoneId = "torso"))

        trackingManager.handle(TimelineIntent.Actor.Unbind)

        // The live document no longer binds the texture...
        assertNull(state.actorDefinition()!!.sceneGraph.bones.getValue("torso").textureBinding)
        // ...but the undo snapshot taken just before the detach still needs it, so a naive
        // "release the moment it's unbound" policy would recycle a bitmap Undo still points at.
        assertTrue(
            "tex-torso" in retained,
            "detached asset must stay reachable while an undo snapshot references it",
        )

        trackingManager.handle(TimelineIntent.Actor.Undo)
        assertEquals("tex-torso", state.actorDefinition()!!.sceneGraph.bones.getValue("torso").textureBinding?.assetId)

        // Once history no longer needs it (redo was consumed and no other snapshot binds it),
        // the sweep is free to drop the asset. Undo again to move it out of redo entirely, then
        // clear history the way switching workspaces does.
        trackingManager.handle(TimelineIntent.Actor.Redo)
        trackingManager.clearHistory()
        assertFalse("tex-torso" in retained, "nothing still binds this texture once history is cleared")
    }

    @Test fun `hiding a part records history and leaves its binding untouched`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        bindTexture("torso", "tex-torso")
        val boundBefore = state.actorDefinition()!!.sceneGraph.bones.getValue("torso").textureBinding

        manager.handle(TimelineIntent.Actor.SetPartVisibility("torso", false))

        val torso = state.actorDefinition()!!.sceneGraph.bones.getValue("torso")
        assertFalse(torso.visible)
        assertEquals(boundBefore, torso.textureBinding, "hiding a part must not touch its texture binding")

        // It went through checkpoint(), so it is undoable like any other rig edit.
        manager.handle(TimelineIntent.Actor.Undo)
        assertTrue(state.actorDefinition()!!.sceneGraph.bones.getValue("torso").visible)
    }

    @Test fun `setting visibility to its current value is a no-op that does not grow history`() {
        manager.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        val before = state.project

        manager.handle(TimelineIntent.Actor.SetPartVisibility("torso", true))

        assertEquals(before, state.project)
        manager.handle(TimelineIntent.Actor.Undo)
        // Undo must land back on the pre-Create state (empty project), proving no spurious
        // checkpoint was pushed for the redundant visibility "change".
        assertTrue(state.project.puppetDefinitions.isEmpty())
    }
}
