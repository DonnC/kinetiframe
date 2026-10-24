package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.model.ActorArtwork
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.PerformanceTiming
import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.motion.BeforeTake
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import com.donnclab.kinetiframe.engine.rigging.motion.MotionComposition
import com.donnclab.kinetiframe.engine.rigging.motion.MotionCurveSampler
import com.donnclab.kinetiframe.engine.rigging.motion.PerformanceClock
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling

/** Deterministic evaluator for project performance documents; rendering never reads files directly. */
object PerformanceEvaluator {
    private val curveSampler = MotionCurveSampler()
    fun pose(document: PerformanceDocument, timeMicros: Long, rest: SceneGraph): SceneGraph {
        val time = normalizeTime(document, timeMicros)
        return ActorArtwork.apply(
            ActorMotionPose.apply(
                rest,
                rotationsAt(document, time, rest),
                rootTranslationsAt(document, time, rest),
                ActorPoseTrack.contacts(document.poseKeys, time),
            ),
            document.artworkKeys,
            time,
        )
    }
    fun rotations(document: PerformanceDocument, timeMicros: Long, rest: SceneGraph): Map<String, Float> =
        rotationsAt(document, normalizeTime(document, timeMicros), rest)

    private fun rotationsAt(document: PerformanceDocument, time: Long, rest: SceneGraph): Map<String, Float> {
        val legacy = ActorPoseTrack.evaluate(
            document.poseKeys,
            time,
            rest,
            PoseSampling(periodicKeys(document), document.poseInterpolation.engineInterpolation()),
        )
        val preset = document.appliedPreset?.offsets(time).orEmpty()
        val curves = curveRotations(document.rotationCurves, time, curveSampling(document))
        return MotionComposition.rotations(rest, legacy, preset, curves)
    }

    private fun rootTranslationsAt(
        document: PerformanceDocument,
        time: Long,
        rest: SceneGraph,
    ): Map<String, RootTranslation> = MotionComposition.rootTranslations(
        ActorPoseTrack.rootTranslations(
            document.poseKeys,
            time,
            rest,
            PoseSampling(periodicKeys(document), document.poseInterpolation.engineInterpolation()),
        ),
        document.appliedPreset?.rootTranslations(time, rest).orEmpty(),
        RootMotionEvaluator.sample(document.rootMotionCurves, time, curveSampling(document)),
    )

    fun normalizeTime(document: PerformanceDocument, timeMicros: Long): Long = PerformanceClock.normalize(
        PerformanceTiming(document.durationMicros, document.playback),
        timeMicros,
        standalonePreset(document),
    )

    private fun standalonePreset(document: PerformanceDocument): PresetTiming? = document.appliedPreset?.takeIf {
        document.poseKeys.isEmpty() && document.rotationCurves.isEmpty() && document.artworkKeys.isEmpty() &&
            document.rootMotionCurves.isEmpty()
    }?.let { preset ->
        PresetTiming(
            preset.durationMicros,
            if (preset.recipe.loop) PresetClockMode.REPEAT else PresetClockMode.ONE_SHOT,
        )
    }

    private fun periodicKeys(document: PerformanceDocument) = document.playback == PerformancePlayback.LOOP &&
        document.poseKeys.firstOrNull()?.timeMicros == 0L &&
        document.poseKeys.lastOrNull()?.timeMicros == document.durationMicros

    fun curveRotations(
        curves: List<RotationCurve>,
        time: Long,
        sampling: CurveSampling = CurveSampling(),
    ): Map<String, Float> = curveSampler.rotations(curves, time, sampling)

    private fun curveSampling(document: PerformanceDocument): CurveSampling = CurveSampling(
        document.playback == PerformancePlayback.LOOP,
        document.durationMicros,
        if (document.presetSnapshot != null) BeforeTake.PRESERVE_PRESET else BeforeTake.HOLD_FIRST_VALUE,
    )
}

/** The saved preset snapshot as an evaluator, shared across frames for the same document. */
internal val PerformanceDocument.appliedPreset: AppliedActorPreset?
    get() = presetSnapshot?.let(AppliedActorPreset::of)
