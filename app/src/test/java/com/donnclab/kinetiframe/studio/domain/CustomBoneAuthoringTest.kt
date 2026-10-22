package com.donnclab.kinetiframe.studio.domain

import com.donnclab.kinetiframe.actors.domain.ActorStudioManager
import com.donnclab.kinetiframe.actors.domain.RiggingManager
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CustomBoneAuthoringTest {
    @Test fun selectedParentWinsOverNearestEndpointAndAdditionCanUndo() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val studio = ActorStudioManager({ state }, { state = it(state) })
        studio.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(selectedBoneId = "head"))
        val before = state.actorDefinition()!!.sceneGraph
        val target = RigGeometry.resolve(before).getValue("left_foot").end
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val manager =
                RiggingManager(scope, RigAssetManager(), { state }, { state = it(state) }, {}, studio::checkpoint)
            manager.addBoneAt(target.x, target.y)
            val added = (state.actorDefinition()!!.sceneGraph.bones - before.bones.keys).values.single()
            assertEquals("head", added.parentId)
            studio.handle(StudioIntent.Actor.Undo)
            assertEquals(before, state.actorDefinition()!!.sceneGraph)
            studio.handle(StudioIntent.Actor.Redo)
            assertTrue(added.id in state.actorDefinition()!!.sceneGraph.bones)
            state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
            val pinned = state.project
            manager.addBoneAt(100f, 100f)
            assertEquals(pinned, state.project)
        } finally {
            scope.cancel()
        }
    }
}
