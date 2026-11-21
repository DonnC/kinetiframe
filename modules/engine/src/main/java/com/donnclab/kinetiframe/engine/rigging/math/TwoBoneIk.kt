// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.cos
import kotlin.math.sqrt

/** Analytic elbow/knee solve with explicit bend side and bounded flexion. Near extension/root
 * targets are clamped into the reachable annulus, never stretched or iteratively side-flipped.
 * Custom chains of more than two segments still use FABRIK.
 */
object TwoBoneIk {
    fun solve(
        joints: List<Point2>,
        target: Point2,
        bendSide: BendSide,
        limits: BendLimits = BendLimits.Default,
    ): List<Point2> {
        val minimumBendDegrees = limits.minimumDegrees
        val maximumBendDegrees = limits.maximumDegrees
        requireIk(joints.size == 3, TwoBoneIssue.CHAIN_SHAPE)
        requireIk((joints + target).all { it.x.isFinite() && it.y.isFinite() }, TwoBoneIssue.NON_FINITE)
        requireIk(minimumBendDegrees in 0f..maximumBendDegrees && maximumBendDegrees <= 180f, TwoBoneIssue.BEND_LIMITS)
        val a = (joints[1] - joints[0]).getDistance().toDouble()
        val b = (joints[2] - joints[1]).getDistance().toDouble()
        requireIk(a > MINIMUM_LENGTH && b > MINIMUM_LENGTH, TwoBoneIssue.SEGMENT_LENGTH)
        val delta = target - joints[0]
        val fallback = joints[2] - joints[0]
        val axis = when {
            delta.getDistance() > MINIMUM_DISTANCE -> delta / delta.getDistance()
            fallback.getDistance() > MINIMUM_DISTANCE -> fallback / fallback.getDistance()
            else -> (joints[1] - joints[0]) / a.toFloat()
        }
        fun radius(bend: Float) =
            sqrt((a * a + b * b + 2 * a * b * cos(Math.toRadians(bend.toDouble()))).coerceAtLeast(0.0))
        // Leave sub-pixel slack at full extension. An exactly straight chain loses its bend
        // sign, so the next separate drag would default to the other side. This imperceptible
        // residual bend preserves the artist's choice without per-character mutable solver state.
        val maximumRadius = minOf(radius(minimumBendDegrees), a + b - maxOf(MINIMUM_LENGTH, (a + b) * EXTENSION_SLACK))
        val minimumRadius = maxOf(MINIMUM_LENGTH, radius(maximumBendDegrees))
        val distance = delta.getDistance().toDouble().coerceIn(minimumRadius, maxOf(minimumRadius, maximumRadius))
        val along = ((a * a - b * b + distance * distance) / (2 * distance)).coerceIn(-a, a)
        val height = sqrt((a * a - along * along).coerceAtLeast(0.0))
        val normal = Point2(-axis.y, axis.x)
        return listOf(
            joints[0],
            joints[0] + axis * along.toFloat() + normal * (height * bendSide.sign).toFloat(),
            joints[0] + axis * distance.toFloat(),
        )
    }

    fun bendSide(joints: List<Point2>): BendSide {
        val axis = joints.last() - joints.first()
        val elbow = joints[1] - joints.first()
        return if (axis.x * elbow.y - axis.y * elbow.x < 0f) BendSide.NEGATIVE else BendSide.POSITIVE
    }

    private const val MINIMUM_LENGTH = .0001
    private const val MINIMUM_DISTANCE = .0001f
    private const val EXTENSION_SLACK = .000001
}
