package com.donnclab.kinetiframe.ui.components.timeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimelineScrubPositionTest {
    @Test fun `scrub selects viewport center not first visible frame`() {
        assertEquals(3, TimelineScrubPosition.nearestCenter(listOf(1 to 20f, 2 to 60f, 3 to 100f, 4 to 140f), 105f))
        assertEquals(0, TimelineScrubPosition.nearestCenter(listOf(0 to 18f, 1 to 58f), 18f))
        assertEquals(7, TimelineScrubPosition.nearestCenter(listOf(6 to -22f, 7 to 18f), 18f))
    }

    @Test fun `fractional scroll maintains the anchored playhead in both directions`() {
        assertEquals(5, TimelineScrubPosition.frame(3.2f, 2.5f, 20))
        assertEquals(6, TimelineScrubPosition.frame(4.2f, 2.5f, 20))
        assertEquals(4, TimelineScrubPosition.frame(2.2f, 2.5f, 20))
    }

    @Test fun `clamps endpoints and excludes add frame cell`() {
        assertEquals(0, TimelineScrubPosition.frame(-4f, .5f, 8))
        assertEquals(7, TimelineScrubPosition.frame(8f, .5f, 8))
        assertEquals(0, TimelineScrubPosition.frame(2f, .5f, 0))
    }
}
