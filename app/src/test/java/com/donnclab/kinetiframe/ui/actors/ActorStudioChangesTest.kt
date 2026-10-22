package com.donnclab.kinetiframe.ui.actors

import com.donnclab.kinetiframe.domain.manager.ActorStudioManager
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset
import com.donnclab.kinetiframe.engine.rigging.presets.RotationChannel
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorStudioChangesTest {
    private fun initial(): StudioState {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        return state
    }

    @Test fun `duration and preset edits require dirty protection`() {
        val initial = initial()
        assertTrue(
            hasActorStudioChanges(
                initial,
                initial.copy(actor = initial.actor.copy(performanceDurationMicros = 3_000_000)),
            ),
        )
        val actor = initial.activeActor()!!
        val preset = AppliedActorPreset(
            ActorMotionPreset(
                id = "nod",
                name = "Nod",
                description = "",
                durationSeconds = 2f,
                requiredBones = listOf("head"),
                channels = listOf(RotationChannel("head", amplitudeDegrees = 5f)),
            ),
        )
        val changed = initial.copy(
            project = initial.project.copy(
                puppetInstances = initial.project.puppetInstances +
                    (actor.id to actor.copy(motionPreset = preset)),
            ),
        )
        assertTrue(hasActorStudioChanges(initial, changed))
        assertFalse(hasActorStudioChanges(initial, initial))
    }

    @Test fun `scrubbing and part selection are not authored edits`() {
        val initial = initial()
        assertFalse(
            hasActorStudioChanges(
                initial,
                initial.copy(
                    actor = initial.actor.copy(previewTimeMicros = 500_000),
                    scene = initial.scene.copy(selectedBoneId = "head"),
                ),
            ),
        )
    }
}
