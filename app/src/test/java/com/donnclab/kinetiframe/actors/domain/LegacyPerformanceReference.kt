package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.model.ActorArtwork
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.motion.BeforeTake
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import com.donnclab.kinetiframe.engine.rigging.motion.MotionCurveSampler
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling

/** Deterministic evaluator for project performance documents; rendering never reads files directly. */
internal object LegacyPerformanceReference {
    private val curveSampler = MotionCurveSampler()
    fun pose(document: PerformanceDocument, timeMicros: Long, rest: SceneGraph): SceneGraph {
        val time = normalizeTime(document, timeMicros)
        return ActorArtwork.apply(
            ActorMotionPose.apply(
                rest,
                rotations(document, timeMicros, rest),
                ActorPoseTrack.rootTranslations(
                    document.poseKeys,
                    time,
                    rest,
                    PoseSampling(periodicKeys(document), document.poseInterpolation.engineInterpolation()),
                ) +
                    document.appliedPreset?.rootTranslations(time, rest).orEmpty() +
                    RootMotionEvaluator.sample(document.rootMotionCurves, time, curveSampling(document)),
                ActorPoseTrack.contacts(document.poseKeys, time),
            ),
            document.artworkKeys,
            time,
        )
    }
    fun rotations(document: PerformanceDocument, timeMicros: Long, rest: SceneGraph): Map<String, Float> {
        val time = normalizeTime(document, timeMicros)
        val legacy = ActorPoseTrack.evaluate(
            document.poseKeys,
            time,
            rest,
            PoseSampling(periodicKeys(document), document.poseInterpolation.engineInterpolation()),
        )
        val preset = document.appliedPreset?.offsets(time)?.mapValues { (boneId, offset) ->
            (rest.bones[boneId]?.localTransform?.rotationDegrees ?: 0f) + offset
        }.orEmpty()
        val curves = curveRotations(document.rotationCurves, time, curveSampling(document))
        // New explicit curves override old key/snapshot channels; each is a durable authored value.
        return legacy + preset + curves
    }

    fun normalizeTime(document: PerformanceDocument, timeMicros: Long): Long {
        // Older saved preset clips used the default 2 s LOOP container even for an 0.8 s
        // one-shot. The recipe owns its motion clock; do not restart it at that outer seam.
        val preset = document.appliedPreset
        if (preset != null && document.poseKeys.isEmpty() && document.rotationCurves.isEmpty() &&
            document.artworkKeys.isEmpty() &&
            document.rootMotionCurves.isEmpty()
        ) {
            return when {
                !preset.recipe.loop -> timeMicros.coerceIn(0, preset.durationMicros)
                document.playback == PerformancePlayback.LOOP -> timeMicros.coerceAtLeast(0) % preset.durationMicros
                else -> timeMicros.coerceIn(0, document.durationMicros)
            }
        }
        return when (document.playback) {
            PerformancePlayback.LOOP -> Math.floorMod(timeMicros, document.durationMicros)

            PerformancePlayback.ONE_SHOT, PerformancePlayback.END_HOLD -> timeMicros.coerceIn(
                0,
                document.durationMicros,
            )
        }
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
