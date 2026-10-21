// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.math

import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FabrikTest {
    private val chain = listOf(Point2(0f, 0f), Point2(100f, 40f), Point2(200f, 0f))

    @Test fun `reachable solve preserves root lengths and input`() {
        val before = chain.toList()
        val target = Point2(130f, 100f)
        val result = Fabrik.solve(chain, target)
        assertEquals(before, chain)
        assertEquals(chain.first(), result.first())
        assertTrue((result.last() - target).getDistance() <= .05f)
        chain.zipWithNext().zip(result.zipWithNext()).forEach { (original, solved) ->
            assertEquals(
                (original.second - original.first).getDistance(),
                (solved.second - solved.first).getDistance(),
                .001f,
            )
        }
    }

    @Test fun `unreachable target never stretches chain`() {
        val straight = listOf(Point2(0f, 0f), Point2(100f, 0f), Point2(200f, 0f))
        assertEquals(straight, Fabrik.solve(straight, Point2(400f, 0f)))
    }

    @Test fun `mirrored authored bend yields mirrored result`() {
        val target = Point2(150f, 70f)
        val first = Fabrik.solve(chain, target)
        val second = Fabrik.solve(chain.map { Point2(it.x, -it.y) }, Point2(target.x, -target.y))
        first.zip(second).forEach { (a, b) ->
            assertEquals(a.x, b.x, .001f)
            assertEquals(a.y, -b.y, .001f)
        }
    }

    @Test fun `invalid and degenerate inputs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Fabrik.solve(chain, Point2(Float.NaN, 0f)) }
        assertThrows(IllegalArgumentException::class.java) {
            Fabrik.solve(listOf(Point2(0f, 0f), Point2(0f, 0f)), Point2(1f, 1f))
        }
    }
}
