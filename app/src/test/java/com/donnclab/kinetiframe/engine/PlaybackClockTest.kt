package com.donnclab.kinetiframe.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaybackClockTest {
    @Test fun `slow render cannot slow time and loop wraps at project duration`() {
        var now = 0L
        val clock = PlaybackClock { now }
        clock.start(10, 24)
        now = 2_500_000_000
        assertEquals(70, clock.frame(192))
        now = 8_000_000_000
        assertEquals(10, clock.frame(192))
        clock.start(3, 24)
        now += 42_000_000
        assertEquals(4, clock.frame(192))
    }

    @Test fun `fps changes keep position rather than jumping back to original baseline`() {
        var now = 0L
        val clock = PlaybackClock { now }
        clock.start(0, 24)
        now = 1_000_000_000
        clock.changeFps(12, 192)
        now = 2_000_000_000
        assertEquals(36, clock.frame(192))
    }
}
