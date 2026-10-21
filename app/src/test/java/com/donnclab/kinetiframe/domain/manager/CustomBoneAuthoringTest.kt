package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.rigging.math.RigGeometry
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.actorDefinition
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
        var state = TimelineState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val studio = ActorStudioManager({ state }, { state = it(state) })
        studio.handle(TimelineIntent.Actor.Create(ActorTemplate.HUMAN))
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
            studio.handle(TimelineIntent.Actor.Undo)
            assertEquals(before, state.actorDefinition()!!.sceneGraph)
            studio.handle(TimelineIntent.Actor.Redo)
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
