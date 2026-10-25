package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling
import com.donnclab.kinetiframe.studio.SceneState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorMotionManager
import com.donnclab.kinetiframe.studio.domain.actorstudio.ActorStudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MotionTimingTest {
    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions(1000, 800))
    private val keys = listOf(
        ActorPoseKey(0, mapOf("head" to 0f), mapOf("torso" to RootTranslation())),
        ActorPoseKey(1_000_000, mapOf("head" to 100f), mapOf("torso" to RootTranslation(100f, 0f))),
    )

    @Test fun `instance conversion retains imported curves and chosen timing`() {
        val curves = PerformanceAuthoring.rotationCurves(keys, 1_000_000, CurveInterpolation.EASE_IN_OUT)
        val instance = com.donnclab.kinetiframe.core.document.PuppetInstance(
            "instance",
            "definition",
            rotationCurves = curves,
            poseInterpolation = CurveInterpolation.EASE_IN_OUT,
        )
        val converted = LegacyActorMigration.performance(
            instance,
            "project",
            "actor",
            "revision",
            "performance",
            "Ease",
            1,
        )!!
        assertEquals(curves, converted.rotationCurves)
        assertEquals(CurveInterpolation.EASE_IN_OUT, converted.poseInterpolation)
    }

    @Test fun `ease is bounded rests at keys and uses shortest angular arc`() {
        val curve = CompiledMotionCurve(listOf(0, 1_000_000), listOf(170f, -170f), angular = true)
        val ease = CompiledMotionCurve.Interpolation.EASE_IN_OUT
        assertEquals(173.125f, curve.sample(250_000, ease), .001f)
        assertEquals(180f, curve.sample(500_000, ease), .001f)
        assertEquals(170f, curve.sample(1, ease), .0001f)
        assertEquals(-170f, curve.sample(1_000_000, ease))
        for (i in 1..999) assertTrue(curve.sample(i * 1000L, ease) in 170f..190f)
    }

    @Test fun `saved timing survives roundtrip and matches roots and rotations`() {
        CurveInterpolation.entries.forEach { mode ->
            val source = PerformanceDocument(
                performanceId = "motion", projectId = "project", actorId = "actor", actorRevisionId = "revision",
                displayName = "Test", durationMicros = 1_000_000, playback = PerformancePlayback.END_HOLD,
                poseKeys = keys, rotationCurves = PerformanceAuthoring.rotationCurves(keys, 1_000_000, mode),
                poseInterpolation = mode, createdAtEpochMs = 1, updatedAtEpochMs = 1,
            ).validate()
            val decoded = Json.decodeFromString<PerformanceDocument>(Json.encodeToString(source))
            assertEquals(source, decoded)
            val engine = mode.engineInterpolation()
            assertEquals(
                ActorPoseTrack.evaluate(
                    keys,
                    250_000,
                    rest,
                    sampling = PoseSampling(interpolation = engine),
                ).getValue("head"),
                PerformanceEvaluator.rotations(decoded, 250_000, rest).getValue("head"),
            )
            val graph = PerformanceEvaluator.pose(decoded, 250_000, rest)
            val shift = ActorPoseTrack.rootTranslations(
                keys,
                250_000,
                rest,
                sampling = PoseSampling(interpolation = engine),
            ).getValue("torso")
            assertEquals(
                rest.bones.getValue("torso").localTransform.x + shift.x,
                graph.bones.getValue("torso").localTransform.x,
            )
        }
    }

    @Test fun `timing choice updates authored curves and is undoable`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val author = ActorStudioManager({ state }, { state = it(state) })
        author.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
        val before = state.project
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val motion = ActorMotionManager(scope, { state }, { state = it(state) }, author::checkpoint)
        motion.handle(StudioIntent.ActorMotion.SetInterpolation(CurveInterpolation.EASE_IN_OUT))
        assertEquals(CurveInterpolation.EASE_IN_OUT, state.activeActor()!!.poseInterpolation)
        author.handle(StudioIntent.Actor.Undo)
        assertEquals(before, state.project)
        scope.cancel()
    }
}
