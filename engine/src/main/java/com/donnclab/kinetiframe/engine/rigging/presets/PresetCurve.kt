// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve

/** Shape-preserving cubic Hermite interpolation; authored extrema must not overshoot a joint. */
object PresetCurve {
    fun evaluate(points: List<RotationPoint>, progress: Double, loop: Boolean): Float {
        if (progress <= 0) return points.first().degrees
        if (progress >= 1) return points.last().degrees
        var lo = 0
        var hi = points.lastIndex
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (points[mid].progress <= progress) lo = mid else hi = mid
        }
        val a = points[lo]
        val b = points[hi]
        val span = (b.progress - a.progress).toDouble()
        val t = (progress - a.progress) / span
        val t2 = t * t
        val t3 = t2 * t
        return (
            (2 * t3 - 3 * t2 + 1) * a.degrees +
                (t3 - 2 * t2 + t) * span * tangent(points, lo, loop) +
                (-2 * t3 + 3 * t2) * b.degrees +
                (t3 - t2) * span * tangent(points, hi, loop)
            ).toFloat()
    }

    private fun tangent(p: List<RotationPoint>, index: Int, loop: Boolean): Double {
        if (!loop && (index == 0 || index == p.lastIndex)) return 0.0
        val i = if (index == p.lastIndex) 0 else index
        val previous = if (i == 0) p[p.lastIndex - 1] else p[i - 1]
        val current = p[i]
        val next = p[i + 1]
        val before = if (i == 0) 1.0 - previous.progress else (current.progress - previous.progress).toDouble()
        val after = (next.progress - current.progress).toDouble()
        val left = (current.degrees - previous.degrees) / before
        val right = (next.degrees - current.degrees) / after
        // Zero velocity at reversals preserves authored poses; harmonic slopes avoid overshoot.
        return CompiledMotionCurve.harmonicTangent(before, after, left, right)
    }
}
