package com.donnclab.kinetiframe.actors.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PerformanceRulerTimingTest {
    @Test fun `frame positions use absolute time without accumulated truncation`() {
        assertEquals(500_000L, PerformanceRulerTiming.timeAt(6, 12, 2_000_000))
        assertEquals(1_000_000L, PerformanceRulerTiming.timeAt(13, 13, 2_000_000))
        assertEquals(390, PerformanceRulerTiming.frameAt(30_000_000, 13))
    }

    @Test fun `partial final frame remains reachable at its exact end time`() {
        assertEquals(7, PerformanceRulerTiming.frameCount(510_000, 12))
        assertEquals(510_000L, PerformanceRulerTiming.timeAt(7, 12, 510_000))
    }
}
