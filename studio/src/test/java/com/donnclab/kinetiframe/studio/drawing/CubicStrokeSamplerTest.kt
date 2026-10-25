package com.donnclab.kinetiframe.studio.drawing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CubicStrokeSamplerTest {
    @Test fun `close endpoints with a curved control polygon are not reduced to one line`() {
        var count = 0
        var furthestY = 0f
        CubicStrokeSampler.forEachEnd(0f, 0f, 0f, 20f, 1f, 20f, 1f, 0f) { _, y, _ ->
            count++
            furthestY = maxOf(furthestY, y)
        }
        assertTrue(count > 2)
        assertEquals(15f, furthestY, 0.001f)
    }

    @Test fun `stationary contacts remain cheap and extreme input stays bounded`() {
        var count = 0
        CubicStrokeSampler.forEachEnd(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f) { _, _, _ -> count++ }
        assertEquals(1, count)
        count = 0
        var lastT = 0f
        CubicStrokeSampler.forEachEnd(0f, 0f, 1e6f, 1e6f, -1e6f, 1e6f, 1f, 0f) { _, _, t ->
            assertTrue(t > lastT)
            lastT = t
            count++
        }
        assertTrue(count <= 256)
        assertEquals(1f, lastT)
    }
}
