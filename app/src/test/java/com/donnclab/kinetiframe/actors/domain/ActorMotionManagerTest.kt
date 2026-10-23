package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.rig.presets.RotationChannel
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootMotionPoint
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActorMotionManagerTest {
    @Test fun `review loops only the recorded take instead of the whole performance`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val actor = state.activeActor()!!
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            actor = state.actor.copy(
                takePhase = PerformanceTakePhase.REVIEW,
                takeStartMicros = 40_000_000,
                takeEndMicros = 41_000_000,
                performanceDurationMicros = 90_000_000,
                previewTimeMicros = 40_000_000,
            ),
            project = state.project.copy(
                puppetInstances = state.project.puppetInstances + (
                    actor.id to actor.copy(
                        rootMotionCurves = listOf(
                            RootMotionCurve(
                                "torso",
                                listOf(
                                    RootMotionPoint(40_000_000, 0f, 0f),
                                    RootMotionPoint(41_000_000, 30f, 0f),
                                ),
                            ),
                        ),
                    )
                    ),
            ),
        )
        val manager =
            ActorMotionManager(backgroundScope, { state }, { state = it(state) }, {}, clockMicros = {
                testScheduler.currentTime *
                    1000
            })
        manager.handle(StudioIntent.ActorMotion.TogglePlayback)
        advanceTimeBy(1500)
        runCurrent()
        assertTrue(state.actor.previewTimeMicros in 40_480_000L..40_520_000L)
        assertTrue(state.actor.previewPlaying)
        manager.stop()
    }

    @Test fun `preset rehearsal uses its period and one shots hold then restart on play`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val instance = state.activeActor()!!
        val recipe = ActorMotionPreset(
            id = "test",
            name = "Test",
            description = "",
            durationSeconds = .8f,
            requiredBones = listOf("head"),
            channels = listOf(
                RotationChannel("head", offsetDegrees = 20f),
            ),
            loop = false,
        )
        state =
            state.copy(
                scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
                project = state.project.copy(
                    puppetInstances =
                    state.project.puppetInstances +
                        (
                            instance.id to
                                instance.copy(
                                    motionPreset = AppliedActorPreset(
                                        recipe,
                                    ).document(),
                                )
                            ),
                ),
            )
        val manager =
            ActorMotionManager(backgroundScope, { state }, { state = it(state) }, {}, clockMicros = {
                testScheduler.currentTime *
                    1000
            })
        assertEquals(800_000L, state.actorPreviewDuration())
        manager.handle(StudioIntent.ActorMotion.TogglePlayback)
        advanceTimeBy(1000)
        runCurrent()
        assertFalse(state.actor.previewPlaying)
        assertEquals(800_000L, state.actor.previewTimeMicros)
        manager.handle(StudioIntent.ActorMotion.TogglePlayback)
        runCurrent()
        assertEquals(0L, state.actor.previewTimeMicros)
        manager.stop()
        state = state.copy(
            project = state.project.copy(
                puppetInstances = state.project.puppetInstances +
                    (
                        instance.id to
                            instance.copy(
                                motionPreset = AppliedActorPreset(
                                    recipe.copy(loop = true),
                                ).document(),
                            )
                        ),
            ),
        )
        manager.handle(StudioIntent.ActorMotion.TogglePlayback)
        advanceTimeBy(850)
        runCurrent()
        assertTrue(state.actor.previewPlaying)
        assertTrue(state.actor.previewTimeMicros in 32_000L..64_000L)
        manager.stop()
    }

    @Test fun `performance authoring permits undo redo of duration and recorded poses`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val authoring = ActorStudioManager({ state }, { state = it(state) })
        authoring.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        authoring.clearHistory()
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            actor = state.actor.copy(isPerformanceAuthoring = true),
        )
        val rest = state.actorDefinition()
        val manager = ActorMotionManager(backgroundScope, { state }, { state = it(state) }, authoring::checkpoint)
        manager.handle(StudioIntent.ActorMotion.SetDuration(3_000_000))
        authoring.handle(StudioIntent.Actor.Undo)
        assertEquals(2_000_000L, state.actor.performanceDurationMicros)
        authoring.handle(StudioIntent.Actor.Redo)
        assertEquals(3_000_000L, state.actor.performanceDurationMicros)
        manager.handle(StudioIntent.ActorMotion.RecordKey)
        authoring.handle(StudioIntent.Actor.Undo)
        assertTrue(state.activeActor()!!.poseKeys.isEmpty())
        authoring.handle(StudioIntent.Actor.Redo)
        assertEquals(1, state.activeActor()!!.poseKeys.size)
        assertEquals(rest, state.actorDefinition())
        authoring.handle(StudioIntent.Actor.Unbind)
        assertEquals(rest, state.actorDefinition())
    }

    @Test fun `duration is frame-authorable and cannot discard an existing key`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val authoring = ActorStudioManager({ state }, { state = it(state) })
        authoring.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
        val manager = ActorMotionManager(backgroundScope, { state }, { state = it(state) }, authoring::checkpoint)

        manager.handle(StudioIntent.ActorMotion.SetDuration(3_000_000L))
        assertEquals(3_000_000L, state.actorPreviewDuration())
        manager.handle(StudioIntent.ActorMotion.Scrub(3_000_000L))
        manager.handle(StudioIntent.ActorMotion.RecordKey)
        manager.handle(StudioIntent.ActorMotion.SetDuration(1_000_000L))

        assertEquals(3_000_000L, state.actorPreviewDuration())
    }

    @Test fun `record scrub playback and stop use isolated actor time`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val authoring = ActorStudioManager({ state }, { state = it(state) })
        authoring.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
        val sceneTime = state.animation
        val manager = ActorMotionManager(
            backgroundScope,
            { state },
            { state = it(state) },
            authoring::checkpoint,
            clockMicros = { testScheduler.currentTime * 1000 },
        )
        manager.handle(StudioIntent.ActorMotion.RecordKey)
        manager.handle(StudioIntent.ActorMotion.Scrub(1_000_000))
        manager.handle(StudioIntent.ActorMotion.RecordKey)
        assertEquals(2, state.activeActor()!!.poseKeys.size)
        manager.handle(StudioIntent.ActorMotion.TogglePlayback)
        runCurrent()
        advanceTimeBy(200)
        runCurrent()
        assertTrue(state.actor.previewTimeMicros > 1_000_000)
        manager.stop()
        val stopped = state.actor.previewTimeMicros
        advanceTimeBy(100)
        assertEquals(stopped, state.actor.previewTimeMicros)
        assertEquals(sceneTime, state.animation)
        manager.handle(StudioIntent.ActorMotion.Scrub(1_000_000))
        manager.handle(StudioIntent.ActorMotion.DeleteKey)
        assertEquals(1, state.activeActor()!!.poseKeys.size)
    }
}
