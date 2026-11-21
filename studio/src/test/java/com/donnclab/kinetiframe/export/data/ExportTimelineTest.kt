package com.donnclab.kinetiframe.export.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportTimelineTest {
    @Test fun `every supported FPS yields increasing frame timestamps without cumulative drift`() {
        for (fps in 1..30) {
            val timestamps = (0L..fps * 120L).map { ExportTimeline.timeUs(it, fps) }
            assertEquals(0L, timestamps.first())
            assertEquals(120_000_000L, timestamps.last())
            assertTrue(timestamps.zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test fun `invalid frame indices and FPS are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ExportTimeline.timeUs(-1, 12) }
        assertThrows(IllegalArgumentException::class.java) { ExportTimeline.timeUs(0, 0) }
    }
}
