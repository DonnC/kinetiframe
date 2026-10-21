// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.acos

class TwoBoneIkTest {
    private val chain = listOf(Point2(0f, 0f), Point2(100f, 0f), Point2(160f, 0f))

    @Test fun `both bend directions preserve root and segment lengths`() {
        BendSide.entries.forEach { side ->
            val target = Point2(80f, 90f)
            val solved = TwoBoneIk.solve(chain, target, side)
            assertEquals(chain.first(), solved.first())
            assertTrue((solved.last() - target).getDistance() < .001f)
            assertEquals(100f, (solved[1] - solved[0]).getDistance(), .001f)
            assertEquals(60f, (solved[2] - solved[1]).getDistance(), .001f)
            assertEquals(side, TwoBoneIk.bendSide(solved))
        }
    }

    @Test fun `extension and root targets stay finite and retain authored bend side`() {
        BendSide.entries.forEach { side ->
            listOf(Point2(1000f, 0f), Point2(0f, 0f)).forEach { target ->
                val result = TwoBoneIk.solve(chain, target, side)
                assertTrue(result.all { it.x.isFinite() && it.y.isFinite() })
                assertEquals(side, TwoBoneIk.bendSide(result))
                assertEquals(100f, (result[1] - result[0]).getDistance(), .01f)
                assertEquals(60f, (result[2] - result[1]).getDistance(), .01f)
            }
        }
    }

    @Test fun `coincident root and tip use upper segment axis fallback`() {
        val folded = listOf(Point2(3f, 4f), Point2(3f, 54f), Point2(3f, 4f))
        val solved = TwoBoneIk.solve(folded, folded.first(), BendSide.NEGATIVE)
        assertTrue(solved.all { it.x.isFinite() && it.y.isFinite() })
        assertEquals(50f, (solved[1] - solved[0]).getDistance(), .001f)
        assertEquals(50f, (solved[2] - solved[1]).getDistance(), .001f)
        assertEquals(BendSide.NEGATIVE, TwoBoneIk.bendSide(solved))
    }

    @Test fun `custom grouped limits clamp flexion without stretching`() {
        val result = TwoBoneIk.solve(chain, Point2(0f, 0f), BendSide.POSITIVE, BendLimits(10f, 30f))
        val radius = (result.last() - result.first()).getDistance().toDouble()
        val bend = Math.toDegrees(acos(((radius * radius - 10000 - 3600) / 12000).coerceIn(-1.0, 1.0)))
        assertEquals(30.0, bend, .001)
    }

    @Test fun `invalid inputs expose typed failures in the original validation order`() {
        assertEquals(
            TwoBoneIssue.CHAIN_SHAPE,
            assertThrows(TwoBoneException::class.java) {
                TwoBoneIk.solve(emptyList(), Point2(Float.NaN, 0f), BendSide.POSITIVE)
            }.issue,
        )
        assertEquals(
            TwoBoneIssue.NON_FINITE,
            assertThrows(TwoBoneException::class.java) {
                TwoBoneIk.solve(chain, Point2(Float.NaN, 0f), BendSide.POSITIVE)
            }.issue,
        )
        assertEquals(
            TwoBoneIssue.BEND_LIMITS,
            assertThrows(TwoBoneException::class.java) {
                TwoBoneIk.solve(chain, Point2(0f, 0f), BendSide.POSITIVE, BendLimits(30f, 10f))
            }.issue,
        )
        assertEquals(
            TwoBoneIssue.SEGMENT_LENGTH,
            assertThrows(TwoBoneException::class.java) {
                TwoBoneIk.solve(List(3) { Point2(0f, 0f) }, Point2(0f, 0f), BendSide.POSITIVE)
            }.issue,
        )
    }

    @Test fun `straight chain tie retains positive legacy bend sign`() {
        assertEquals(BendSide.POSITIVE, TwoBoneIk.bendSide(chain))
        assertEquals(-1, BendSide.NEGATIVE.sign)
        assertEquals(1, BendSide.POSITIVE.sign)
    }
}
