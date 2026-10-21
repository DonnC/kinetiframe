package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorView
import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import com.donnclab.kinetiframe.engine.rigging.presets.RotationChannel
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineEffect
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.activeActor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorPresetManagerTest {
    private val recipe = ActorMotionPreset(
        id = "nod",
        name = "Nod",
        description = "Test",
        durationSeconds = 2f,
        requiredBones = listOf("head"),
        channels = listOf(RotationChannel("head", amplitudeDegrees = 10f)),
    )
    private var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
    private val effects = mutableListOf<TimelineEffect>()
    private val authoring = ActorStudioManager({ state }, { state = it(state) })
    private val manager = ActorPresetManager({ state }, { state = it(state) }, authoring::checkpoint, effects::add)

    private fun create() {
        authoring.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
        state =
            state.copy(
                scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
                actor = state.actor.copy(presets = listOf(recipe)),
            )
    }

    @Test fun `cancel does not change document and applied recipe can be undone`() {
        create()
        val before = state.project
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        manager.handle(TimelineIntent.ActorPreset.SetIntensity(1.5f))
        assertEquals(before, state.project)
        manager.handle(TimelineIntent.ActorPreset.Cancel)
        assertNull(state.actor.presetPreview)
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        manager.handle(TimelineIntent.ActorPreset.Apply)
        assertEquals(recipe, state.activeActor()!!.motionPreset!!.recipe)
        assertTrue(state.activeActor()!!.poseKeys.isEmpty())
        authoring.handle(TimelineIntent.Actor.Undo)
        assertEquals(before, state.project)
    }

    @Test fun `matching bone roles cannot apply a front recipe to a profile view`() {
        create()
        state = state.copy(actor = state.actor.copy(authoredView = ActorView.PROFILE))
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        assertNull(state.actor.presetPreview)
        assertEquals(1, effects.size)
        assertTrue(recipe.supportsView(ActorView.FRONT))
        assertTrue(recipe.supportsView(ActorView.ORIGINAL))
        assertFalse(recipe.supportsView(ActorView.BACK))
    }

    @Test fun `manual keys and missing capabilities are never silently overwritten`() {
        create()
        state = state.copy(actor = state.actor.copy(presets = listOf(recipe.copy(requiredBones = listOf("wings")))))
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        assertNull(state.actor.presetPreview)
        assertEquals(1, effects.size)
        val instance = state.activeActor()!!
        state = state.copy(
            actor = state.actor.copy(presets = listOf(recipe)),
            project = state.project.copy(
                puppetInstances = mapOf(instance.id to instance.copy(poseKeys = listOf(ActorPoseKey(0, emptyMap())))),
            ),
        )
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        assertNotNull(state.actor.presetPreview)
        assertEquals(1, effects.size)
        assertEquals(1, state.activeActor()!!.poseKeys.size)
        manager.handle(TimelineIntent.ActorPreset.Cancel)
        assertEquals(1, state.activeActor()!!.poseKeys.size)
        manager.handle(TimelineIntent.ActorPreset.Preview("nod"))
        manager.handle(TimelineIntent.ActorPreset.Apply)
        assertTrue(state.activeActor()!!.poseKeys.isEmpty())
        authoring.handle(TimelineIntent.Actor.Undo)
        assertEquals(1, state.activeActor()!!.poseKeys.size)
    }
}
