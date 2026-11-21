// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
@SerialName("com.kinetiframe.data.actor.RootMotionPoint")
data class RootMotionPoint(val timeMicros: Long, val x: Float, val y: Float)

/** Actor-space displacement from the immutable root, never screen coordinates. */
@Serializable
@SerialName("com.kinetiframe.data.actor.RootMotionCurve")
data class RootMotionCurve(
    val rootId: String,
    val points: List<RootMotionPoint>,
    val interpolation: CurveInterpolation = CurveInterpolation.SMOOTH,
) {
    fun requireValid(duration: Long): RootMotionCurve = apply {
        requireMotion(rootId.isNotBlank(), MotionValidationIssue.EMPTY_PART_ID)
        requireMotion(
            points.isNotEmpty() && points.size <= RotationCurve.MAX_POINTS,
            MotionValidationIssue.ROOT_POINT_BUDGET,
        )
        requireMotion(
            points.zipWithNext().all { (a, b) ->
                a.timeMicros < b.timeMicros
            },
            MotionValidationIssue.UNORDERED_POINTS,
        )
        requireMotion(
            points.all {
                it.timeMicros in 0..duration && it.x.isFinite() && it.y.isFinite() &&
                    abs(it.x) <= MAX_DISPLACEMENT && abs(it.y) <= MAX_DISPLACEMENT
            },
            MotionValidationIssue.INVALID_ROOT_POINT,
        )
    }
    companion object {
        private const val MAX_DISPLACEMENT = 1_000_000f
    }
}
