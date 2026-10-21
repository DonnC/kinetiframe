// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
@SerialName("com.kinetiframe.data.actor.RotationCurvePoint")
data class RotationCurvePoint(val timeMicros: Long, val degrees: Float)

/** Immutable authored channel. Validation never sorts, truncates or changes authored motion. */
@Serializable
@SerialName("com.kinetiframe.data.actor.RotationCurve")
data class RotationCurve(
    val boneId: String,
    val interpolation: CurveInterpolation = CurveInterpolation.LINEAR,
    val points: List<RotationCurvePoint>,
) {
    fun requireValid(durationMicros: Long): RotationCurve = apply {
        requireMotion(boneId.isNotBlank(), MotionValidationIssue.EMPTY_PART_ID)
        requireMotion(points.isNotEmpty(), MotionValidationIssue.EMPTY_POINTS)
        requireMotion(points.size <= MAX_POINTS, MotionValidationIssue.POINT_LIMIT)
        requireMotion(
            points.zipWithNext().all { (a, b) ->
                a.timeMicros < b.timeMicros
            },
            MotionValidationIssue.UNORDERED_POINTS,
        )
        requireMotion(
            points.all {
                it.timeMicros in 0..durationMicros && it.degrees.isFinite() &&
                    abs(it.degrees) <= MAX_DEGREES
            },
            MotionValidationIssue.INVALID_ROTATION_POINT,
        )
    }
    companion object {
        const val MAX_POINTS = 720
        private const val MAX_DEGREES = 360f
    }
}
