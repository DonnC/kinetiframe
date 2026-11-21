// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresetCurveTest {
    private val points = listOf(
        RotationPoint(0f, 0f),
        RotationPoint(.3f, 20f),
        RotationPoint(.7f, -10f),
        RotationPoint(1f, 0f),
    )

    @Test fun `endpoints clamp and knots retain authored angles`() {
        listOf(false, true).forEach { loop ->
            assertEquals(0f, PresetCurve.evaluate(points, -1.0, loop))
            assertEquals(0f, PresetCurve.evaluate(points, 2.0, loop))
            points.forEach { assertEquals(it.degrees, PresetCurve.evaluate(points, it.progress.toDouble(), loop)) }
        }
    }

    @Test fun `each segment stays within authored extrema`() {
        listOf(false, true).forEach { loop ->
            points.zipWithNext().forEach { (left, right) ->
                for (step in 0..100) {
                    val progress = left.progress.toDouble() + (right.progress - left.progress) * step / 100.0
                    val value = PresetCurve.evaluate(points, progress, loop)
                    assertTrue(value >= minOf(left.degrees, right.degrees) - .0001f)
                    assertTrue(value <= maxOf(left.degrees, right.degrees) + .0001f)
                }
            }
        }
    }

    @Test fun `one shot settles and periodic seam keeps velocity`() {
        val epsilon = .00001
        assertEquals(0.0, (PresetCurve.evaluate(points, epsilon, false) - points.first().degrees) / epsilon, .1)
        assertEquals(0.0, (points.last().degrees - PresetCurve.evaluate(points, 1 - epsilon, false)) / epsilon, .1)
        val incoming = (points.last().degrees - PresetCurve.evaluate(points, 1 - epsilon, true)) / epsilon
        val outgoing = (PresetCurve.evaluate(points, epsilon, true) - points.first().degrees) / epsilon
        assertEquals(incoming, outgoing, .1)
    }
}
