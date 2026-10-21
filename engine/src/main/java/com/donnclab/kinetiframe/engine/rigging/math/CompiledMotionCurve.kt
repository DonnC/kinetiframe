// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import kotlin.math.abs

/** Immutable, precompiled PCHIP channel. O(log n) sampling; no per-frame sorting or spline arrays.
 * Harmonic tangents preserve authored extrema/holds (unconstrained cubic splines can overshoot a
 * knee). Endpoint tangents rest at zero except a genuinely matching periodic seam.
 */
class CompiledMotionCurve(times: List<Long>, values: List<Float>, angular: Boolean = false, loop: Boolean = false) {
    enum class Interpolation { SMOOTH, EASE_IN_OUT, LINEAR, STEP }
    private val times = times.toLongArray()
    private val originalValues = values.toFloatArray()
    private val angular = angular
    private val values = DoubleArray(values.size)
    private val slopes = DoubleArray(values.size)

    init {
        require(times.isNotEmpty() && times.size == values.size)
        require(times.zipWithNext().all { (a, b) -> a < b } && values.all(Float::isFinite))
        this.values[0] = values[0].toDouble()
        for (i in 1..values.lastIndex) {
            this.values[i] = if (angular) {
                this.values[i - 1] + shortestAngle(values[i] - values[i - 1])
            } else {
                values[i].toDouble()
            }
        }
        for (i in 1 until values.lastIndex) {
            slopes[i] = harmonicTangent(
                (times[i] - times[i - 1]).toDouble(),
                (times[i + 1] - times[i]).toDouble(),
                (this.values[i] - this.values[i - 1]) / (times[i] - times[i - 1]),
                (this.values[i + 1] - this.values[i]) / (times[i + 1] - times[i]),
            )
        }
        val seamDifference = if (angular) {
            shortestAngle(values.last() - values.first())
        } else {
            values.last() -
                values.first()
        }
        if (loop && values.size > 2 && abs(seamDifference) < PERIODIC_SEAM_EPSILON) {
            val before = (times.last() - times[times.lastIndex - 1]).toDouble()
            val after = (times[1] - times[0]).toDouble()
            val tangent = harmonicTangent(
                before,
                after,
                (this.values.last() - this.values[this.values.lastIndex - 1]) / before,
                (this.values[1] - this.values[0]) / after,
            )
            slopes[0] = tangent
            slopes[slopes.lastIndex] = tangent
        }
    }

    fun sample(time: Long, interpolation: Interpolation = Interpolation.SMOOTH): Float {
        if (time <= times.first()) return originalValues.first()
        if (time >= times.last()) return originalValues.last()
        var lo = 0
        var hi = times.lastIndex
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (times[mid] <= time) lo = mid else hi = mid
        }
        val span = (times[hi] - times[lo]).toDouble()
        val t = (time - times[lo]) / span
        if (time == times[lo] || interpolation == Interpolation.STEP) return originalValues[lo]
        if (interpolation == Interpolation.LINEAR) return (originalValues[lo] + (values[hi] - values[lo]) * t).toFloat()
        // Acting beats rest at every authored key; continuous motion uses PCHIP instead.
        // Smoothstep stays bounded and follows the shortest angular arc without overshoot.
        if (interpolation == Interpolation.EASE_IN_OUT) {
            return (originalValues[lo] + (values[hi] - values[lo]) * t * t * (3 - 2 * t)).toFloat()
        }
        val t2 = t * t
        val t3 = t2 * t
        val sampled = (2 * t3 - 3 * t2 + 1) * values[lo] + (t3 - 2 * t2 + t) * span * slopes[lo] +
            (-2 * t3 + 3 * t2) * values[hi] + (t3 - t2) * span * slopes[hi]
        return (sampled - if (angular) values[lo] - originalValues[lo] else 0.0).toFloat()
    }

    companion object {
        private const val HALF_TURN = 180f
        private const val FULL_TURN = 360f
        private const val PERIODIC_SEAM_EPSILON = .001f
        fun shortestAngle(delta: Float): Float = ((delta + HALF_TURN) % FULL_TURN + FULL_TURN) % FULL_TURN - HALF_TURN
        fun harmonicTangent(before: Double, after: Double, left: Double, right: Double): Double {
            if (left * right <= 0) return 0.0
            val w1 = 2 * after + before
            val w2 = after + 2 * before
            return (w1 + w2) / (w1 / left + w2 / right)
        }
    }
}
