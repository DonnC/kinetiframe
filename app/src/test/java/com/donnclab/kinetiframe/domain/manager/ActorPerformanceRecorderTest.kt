package com.donnclab.kinetiframe.domain.manager

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.data.actor.RotationCurve
import com.donnclab.kinetiframe.data.actor.RotationCurvePoint
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.ui.PerformanceTakePhase
import com.donnclab.kinetiframe.ui.SceneState
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioMode
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.activeActor
import com.donnclab.kinetiframe.ui.actorDefinition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActorPerformanceRecorderTest {
    @Test fun `a thirty second travel take can start forty seconds into a performance and survive save`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val studio = ActorStudioManager({ state }, { state = it(state) })
        studio.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            actor = state.actor.copy(previewTimeMicros = 40_000_000, performanceDurationMicros = 40_000_000),
        )
        val original = state.activeActor()!!
        val rest = state.actorDefinition()!!.sceneGraph
        val root = rest.rootBoneIds.first()
        val recorder =
            ActorPerformanceRecorder(backgroundScope, { state }, { state = it(state) }, {}, {}, {
                testScheduler.currentTime *
                    1000
            })
        recorder.handle(StudioIntent.ActorMotion.StartTake)
        recorder.handle(StudioIntent.ActorMotion.SetTakeMoveCharacter(true))
        val drag = StudioIntent.Actor.Drag("origin:$root", Offset(700f, 500f))
        assertFalse(recorder.beforeDrag(drag))
        studio.handle(drag)
        recorder.afterDrag()
        studio.clearMotionDragBaseline()
        advanceTimeBy(30_100)
        runCurrent()
        assertEquals(PerformanceTakePhase.REVIEW, state.actor.takePhase)
        assertEquals(70_000_000L, state.actor.performanceDurationMicros)
        assertEquals(original.rotationCurves, state.activeActor()!!.rotationCurves)
        assertTrue(state.activeActor()!!.rootMotionCurves.isNotEmpty())
        recorder.handle(StudioIntent.ActorMotion.KeepTake)
        val actor = state.activeActor()!!
        val document = com.donnclab.kinetiframe.domain.actor.LegacyActorMigration.performance(
            actor,
            "project",
            "actor",
            "revision",
            "take",
            "Travel",
            1,
        )!!
        val time = 65_000_000L
        val expected = state.copy(actor = state.actor.copy(previewTimeMicros = time)).actorDefinition(true)!!.sceneGraph
        val saved = com.donnclab.kinetiframe.domain.actor.PerformanceEvaluator.pose(document, time, rest)
        assertEquals(expected.bones.getValue(root).localTransform, saved.bones.getValue(root).localTransform)
    }

    @Test fun `applied preset remains intact while a touched joint records over it`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val actor = state.activeActor()!!
        val recipe = com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset(
            id = "idle",
            name = "Idle",
            description = "",
            durationSeconds = 2f,
            requiredBones = listOf("head"),
            channels = listOf(
                com.donnclab.kinetiframe.engine.rigging.presets.RotationChannel("head", amplitudeDegrees = 12f),
            ),
        )
        val preset = com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset(recipe)
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (actor.id to actor.copy(motionPreset = preset)),
            ),
        )
        val recorder =
            ActorPerformanceRecorder(backgroundScope, { state }, { state = it(state) }, {}, {}, {
                testScheduler.currentTime *
                    1000
            })
        recorder.handle(StudioIntent.ActorMotion.StartTake)
        assertEquals(PerformanceTakePhase.ARMED, state.actor.takePhase)
        recorder.beforeDrag(StudioIntent.Actor.Drag("torso", Offset.Zero))
        val graph = state.actor.previewGraph!!
        state = state.copy(
            actor = state.actor.copy(
                previewGraph = graph.copy(
                    bones = graph.bones +
                        (
                            "torso" to
                                graph.bones.getValue("torso").let {
                                    it.withRest(localTransform = it.localTransform.copy(rotationDegrees = 25f))
                                }
                            ),
                ),
            ),
        )
        recorder.afterDrag()
        advanceTimeBy(300)
        runCurrent()
        recorder.handle(StudioIntent.ActorMotion.StopTake)
        assertEquals(preset, state.activeActor()!!.motionPreset)
        assertTrue(state.activeActor()!!.rotationCurves.none { it.boneId == "head" })
        recorder.cancel()
    }

    @Test fun `artwork passes are discrete and preserve switches on untouched parts`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val actor = state.activeActor()!!
        val ids = state.actorDefinition()!!.sceneGraph.bones.keys.toList()
        val existing = com.donnclab.kinetiframe.engine.rigging.model.ActorArtworkKey(100_000, mapOf(ids[0] to ""))
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (actor.id to actor.copy(artworkKeys = listOf(existing))),
            ),
        )
        val recorder =
            ActorPerformanceRecorder(backgroundScope, { state }, { state = it(state) }, {}, {}, {
                testScheduler.currentTime *
                    1000
            })
        recorder.handle(StudioIntent.ActorMotion.StartTake)
        recorder.selectArtwork(StudioIntent.Actor.SelectArtwork(ids[1], ""))
        advanceTimeBy(200)
        runCurrent()
        recorder.handle(StudioIntent.ActorMotion.StopTake)
        assertTrue(state.activeActor()!!.artworkKeys.contains(existing))
        assertTrue(state.activeActor()!!.artworkKeys.any { ids[1] in it.selections })
        recorder.handle(StudioIntent.ActorMotion.KeepTake)
        assertTrue(state.activeActor()!!.rotationCurves.isEmpty())
        assertEquals(PerformanceTakePhase.IDLE, state.actor.takePhase)
    }

    @Test fun `take waits for first touch then keeps one undoable layered performance`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val studio = ActorStudioManager({ state }, { state = it(state) })
        studio.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        studio.clearHistory()
        val actor = state.activeActor()!!
        val head =
            RotationCurve("head", points = listOf(RotationCurvePoint(0, -90f), RotationCurvePoint(2_000_000, -70f)))
        state = state.copy(
            scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (actor.id to actor.copy(rotationCurves = listOf(head))),
            ),
        )
        val before = state.activeActor()!!
        val rest = state.actorDefinition()!!
        var checkpoints = 0
        val recorder = ActorPerformanceRecorder(
            backgroundScope,
            { state },
            { state = it(state) },
            {
                checkpoints++
                studio.checkpoint()
            },
            {},
            { testScheduler.currentTime * 1000 },
        )
        recorder.handle(StudioIntent.ActorMotion.StartTake)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(0L, state.actor.previewTimeMicros)
        assertEquals(PerformanceTakePhase.ARMED, state.actor.takePhase)
        val id = rest.sceneGraph.bones.keys.first { it != "head" }
        recorder.beforeDrag(StudioIntent.Actor.Drag(id, Offset.Zero))
        val graph = state.actor.previewGraph!!
        state = state.copy(
            actor = state.actor.copy(
                previewGraph = graph.copy(
                    bones = graph.bones +
                        (
                            id to
                                graph.bones.getValue(id).let {
                                    it.withRest(localTransform = it.localTransform.copy(rotationDegrees = 25f))
                                }
                            ),
                ),
            ),
        )
        recorder.afterDrag()
        advanceTimeBy(300)
        runCurrent()
        recorder.release()
        assertEquals(before, state.activeActor()) // Capture does not publish document mutations.
        recorder.handle(StudioIntent.ActorMotion.StopTake)
        assertEquals(PerformanceTakePhase.REVIEW, state.actor.takePhase)
        assertSame(head, state.activeActor()!!.rotationCurves.first { it.boneId == "head" })
        recorder.handle(StudioIntent.ActorMotion.KeepTake)
        assertEquals(1, checkpoints)
        assertEquals(PerformanceTakePhase.IDLE, state.actor.takePhase)
        assertEquals(rest, state.actorDefinition())
        studio.handle(StudioIntent.Actor.Undo)
        assertEquals(before, state.activeActor())
        studio.handle(StudioIntent.Actor.Redo)
        assertTrue(state.activeActor()!!.rotationCurves.any { it.boneId == id })
    }

    @Test fun `cancel restores original duration and cap automatically enters review`() = runTest {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state =
            state.copy(
                scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK),
                actor = state.actor.copy(previewTimeMicros = 119_900_000),
            )
        val original = state.activeActor()
        val recorder =
            ActorPerformanceRecorder(backgroundScope, { state }, { state = it(state) }, {}, {}, {
                testScheduler.currentTime *
                    1000
            })
        recorder.handle(StudioIntent.ActorMotion.StartTake)
        val id = state.actorDefinition()!!.sceneGraph.bones.keys.first()
        recorder.beforeDrag(StudioIntent.Actor.Drag(id, Offset.Zero))
        val graph = state.actor.previewGraph!!
        state = state.copy(
            actor = state.actor.copy(
                previewGraph = graph.copy(
                    bones = graph.bones +
                        (
                            id to
                                graph.bones.getValue(id).let {
                                    it.withRest(localTransform = it.localTransform.copy(rotationDegrees = 25f))
                                }
                            ),
                ),
            ),
        )
        recorder.afterDrag()
        advanceTimeBy(200)
        runCurrent()
        assertEquals(PerformanceTakePhase.REVIEW, state.actor.takePhase)
        assertEquals(120_000_000L, state.actor.performanceDurationMicros)
        recorder.handle(StudioIntent.ActorMotion.Retake)
        assertEquals(original, state.activeActor())
        assertEquals(PerformanceTakePhase.ARMED, state.actor.takePhase)
        recorder.cancel()
        assertEquals(2_000_000L, state.actor.performanceDurationMicros)
        assertEquals(original, state.activeActor())
    }
}
