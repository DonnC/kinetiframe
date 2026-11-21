package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurvePoint

/**
 * Converts the editor's sparse pose snapshots into the durable curve representation.
 *
 * Pose keys are intentionally retained in documents during the schema-1 transition, while
 * curves are the canonical, per-part representation used by new authoring and render paths.
 */
object PerformanceAuthoring {
    fun refineRoots(curves: List<RootMotionCurve>, key: ActorPoseKey) = curves.map { curve ->
        val value = key.rootTranslations[curve.rootId] ?: return@map curve
        val retained = curve.points.filterNot { it.timeMicros == key.timeMicros }
        if (retained.size >= RotationCurve.MAX_POINTS) {
            curve
        } else {
            curve.copy(
                points =
                (
                    retained + RootMotionPoint(
                        key.timeMicros,
                        value.x,
                        value.y,
                    )
                    ).sortedBy {
                    it.timeMicros
                },
            )
        }
    }
    fun rootCurves(
        keys: List<ActorPoseKey>,
        duration: Long,
        interpolation: CurveInterpolation,
    ): List<RootMotionCurve> = keys.flatMap { key ->
        key.rootTranslations.map { (id, value) ->
            id to RootMotionPoint(key.timeMicros, value.x, value.y)
        }
    }.groupBy({ it.first }, { it.second }).map { (id, points) ->
        RootMotionCurve(id, points, interpolation).validate(duration)
    }

    /** Manual refinement must not discard previously recorded per-part takes. */
    fun refineCurves(curves: List<RotationCurve>, key: ActorPoseKey): List<RotationCurve> = curves.map { curve ->
        val value = key.rotations[curve.boneId] ?: return@map curve
        val retained = curve.points.filterNot { it.timeMicros == key.timeMicros }
        // Refuse extra refinement at the cap rather than silently erasing the take.
        if (retained.size >= RotationCurve.MAX_POINTS) {
            curve
        } else {
            curve.copy(
                points =
                (retained + RotationCurvePoint(key.timeMicros, value)).sortedBy { it.timeMicros },
            )
        }
    }
    fun rotationCurves(
        keys: List<ActorPoseKey>,
        durationMicros: Long,
        interpolation: CurveInterpolation = CurveInterpolation.SMOOTH,
    ): List<RotationCurve> {
        require(durationMicros > 0) { "Performance duration must be positive" }
        require(keys.zipWithNext().all { (left, right) -> left.timeMicros < right.timeMicros }) {
            "Pose keys must be ordered uniquely"
        }
        return keys
            .flatMap { key ->
                key.rotations.map { (partId, degrees) ->
                    partId to
                        RotationCurvePoint(key.timeMicros, degrees)
                }
            }
            .groupBy({ it.first }, { it.second })
            .map { (partId, points) ->
                RotationCurve(
                    boneId = partId,
                    interpolation = interpolation,
                    points = points,
                ).validate(durationMicros)
            }
            .sortedBy(RotationCurve::boneId)
    }
}
