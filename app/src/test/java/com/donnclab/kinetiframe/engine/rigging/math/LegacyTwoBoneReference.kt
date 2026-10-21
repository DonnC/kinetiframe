package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sqrt

// Frozen pre-extraction Offset arithmetic provides an independent adapter parity oracle.
internal object LegacyTwoBoneReference {
    fun solve(joints: List<Offset>, target: Offset, bendSide: Int, limits: BendLimits = BendLimits()): List<Offset> {
        val minimumBendDegrees = limits.minimumDegrees
        val maximumBendDegrees = limits.maximumDegrees
        require(joints.size == 3 && bendSide in listOf(-1, 1))
        require((joints + target).all { it.x.isFinite() && it.y.isFinite() })
        require(minimumBendDegrees in 0f..maximumBendDegrees && maximumBendDegrees <= 180f)
        val a = (joints[1] - joints[0]).getDistance().toDouble()
        val b = (joints[2] - joints[1]).getDistance().toDouble()
        require(a > .0001 && b > .0001)
        val delta = target - joints[0]
        val fallback = joints[2] - joints[0]
        val axis = when {
            delta.getDistance() > .0001f -> delta / delta.getDistance()
            fallback.getDistance() > .0001f -> fallback / fallback.getDistance()
            else -> (joints[1] - joints[0]) / a.toFloat()
        }
        fun radius(bend: Float) =
            sqrt((a * a + b * b + 2 * a * b * cos(Math.toRadians(bend.toDouble()))).coerceAtLeast(0.0))
        // Leave sub-pixel slack at full extension. An exactly straight chain loses its bend
        // sign, so the next separate drag would default to the other side. This imperceptible
        // residual bend preserves the artist's choice without per-character mutable solver state.
        val maximumRadius = minOf(radius(minimumBendDegrees), a + b - maxOf(.0001, (a + b) * .000001))
        val minimumRadius = maxOf(.0001, radius(maximumBendDegrees))
        val distance = delta.getDistance().toDouble().coerceIn(minimumRadius, maxOf(minimumRadius, maximumRadius))
        val along = ((a * a - b * b + distance * distance) / (2 * distance)).coerceIn(-a, a)
        val height = sqrt((a * a - along * along).coerceAtLeast(0.0))
        val normal = Offset(-axis.y, axis.x)
        return listOf(
            joints[0],
            joints[0] + axis * along.toFloat() + normal * (height * bendSide).toFloat(),
            joints[0] + axis * distance.toFloat(),
        )
    }

    fun bendSide(joints: List<Offset>): Int {
        val axis = joints.last() - joints.first()
        val elbow = joints[1] - joints.first()
        return if (axis.x * elbow.y - axis.y * elbow.x < 0f) -1 else 1
    }
}
