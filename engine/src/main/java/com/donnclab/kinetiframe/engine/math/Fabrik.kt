// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.core.geometry.Point2

/** Bounded, deterministic FABRIK. Never mutates the input or stretches an unreachable chain. */
object Fabrik {
    const val DEFAULT_ITERATIONS = 24
    const val DEFAULT_TOLERANCE = 0.05f
    private const val MINIMUM_SEGMENT = 0.0001f
    private const val COLLINEAR_EPSILON = 0.001f
    private const val BEND_SEED_RATIO = 0.05f

    fun solve(
        joints: List<Point2>,
        target: Point2,
        iterations: Int = DEFAULT_ITERATIONS,
        tolerance: Float = DEFAULT_TOLERANCE,
    ): List<Point2> {
        require(joints.size >= 2 && iterations > 0 && tolerance > 0)
        require((joints + target).all { it.x.isFinite() && it.y.isFinite() })
        val lengths = joints.zipWithNext { a, b -> (b - a).getDistance() }
        require(lengths.all { it > MINIMUM_SEGMENT })
        val root = joints.first()
        val points = joints.toMutableList()
        fun along(from: Point2, toward: Point2, length: Float, fallback: Point2): Point2 {
            val delta = toward - from
            val direction = if (delta.getDistance() > MINIMUM_SEGMENT) {
                delta / delta.getDistance()
            } else {
                fallback / fallback.getDistance()
            }
            return from + direction * length
        }
        if ((target - root).getDistance() >= lengths.sum()) {
            for (i in lengths.indices) points[i + 1] = along(points[i], target, lengths[i], joints[i + 1] - joints[i])
            return points
        }
        // A perfectly straight chain cannot choose a bend side. Seed a tiny perpendicular bend
        // using a stable side; already bent chains retain their authored elbow/knee direction.
        if (points.size > 2 && points.drop(1).dropLast(1).all {
                val axis = points.last() - root
                val relative = it - root
                kotlin.math.abs(axis.x * relative.y - axis.y * relative.x) < COLLINEAR_EPSILON
            }
        ) {
            val axis = points.last() - root
            val normal = if (axis.getDistance() >
                COLLINEAR_EPSILON
            ) {
                Point2(-axis.y, axis.x) / axis.getDistance()
            } else {
                Point2(0f, 1f)
            }
            points[1] = points[1] + normal * (lengths.first() * BEND_SEED_RATIO)
        }
        repeat(iterations) {
            points[points.lastIndex] = target
            for (i in lengths.indices.reversed()) {
                points[i] =
                    along(points[i + 1], points[i], lengths[i], joints[i] - joints[i + 1])
            }
            points[0] = root
            for (i in lengths.indices) {
                points[i + 1] =
                    along(points[i], points[i + 1], lengths[i], joints[i + 1] - joints[i])
            }
            if ((points.last() - target).getDistance() <= tolerance) return points
        }
        return points
    }
}
