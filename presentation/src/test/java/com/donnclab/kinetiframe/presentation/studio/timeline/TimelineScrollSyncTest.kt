package com.donnclab.kinetiframe.presentation.studio.timeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class TimelineScrollSyncTest {
    private val cellWidthPx = 120f // e.g. 40.dp at 3x density

    @Test fun `pixels exactly on a cell boundary map to that item with zero offset`() {
        assertEquals(0 to 0, TimelineScrollSync.pixelsToItem(0, cellWidthPx))
        assertEquals(5 to 0, TimelineScrollSync.pixelsToItem(600, cellWidthPx))
    }

    @Test fun `pixels partway through a cell keep that remainder as the item's own offset`() {
        assertEquals(2 to 30, TimelineScrollSync.pixelsToItem(270, cellWidthPx))
    }

    @Test fun `negative or non-finite pixel positions clamp to the very start of the ruler`() {
        assertEquals(0 to 0, TimelineScrollSync.pixelsToItem(-500, cellWidthPx))
        assertEquals(0 to 0, TimelineScrollSync.pixelsToItem(100, 0f))
    }

    @Test fun `item-to-pixel is the exact inverse of pixel-to-item across a full sweep`() {
        // This is the property the lane/filmstrip sync actually leans on: converting a raw scroll
        // position to (index, offset) and back must reproduce the same pixel value, or the two
        // scrollables would slowly drift apart every time a scroll event round-trips between them.
        for (px in 0..2_000 step 7) {
            val (index, offset) = TimelineScrollSync.pixelsToItem(px, cellWidthPx)
            assertEquals(px, TimelineScrollSync.itemToPixels(index, offset, cellWidthPx), "px=$px")
        }
    }

    @Test fun `item-to-pixel is the exact inverse of pixel-to-item at fractional cell widths too`() {
        // Real screen densities rarely land FRAME_CELL_WIDTH on a whole pixel -- this guards the
        // rounding in both directions still agrees with itself at an ugly, non-integral width.
        val fractional = 106.66667f // 40.dp at 2.66667x density, a real Android density bucket
        for (px in 0..1_500 step 11) {
            val (index, offset) = TimelineScrollSync.pixelsToItem(px, fractional)
            val roundTripped = TimelineScrollSync.itemToPixels(index, offset, fractional)
            assertTrue(abs(px - roundTripped) <= 1, "px=$px roundTripped=$roundTripped")
        }
    }
}
