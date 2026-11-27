package com.donnclab.kinetiframe.core.document

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CelExposureIndexTest {
    private val heldToEnd = mapOf(0 to "bg", 1 to "bg", 2 to "bg")

    @Test fun `a frame added after a hold that reaches the end joins it`() {
        assertEquals((0..3).associateWith { "bg" }, CelExposureIndex.insertContinuingHold(heldToEnd, 3, 3))
    }

    @Test fun `a frame added inside a hold joins it`() {
        assertEquals((0..3).associateWith { "bg" }, CelExposureIndex.insertContinuingHold(heldToEnd, 1, 3))
    }

    @Test fun `a frame added after a hold that stops early stays empty`() {
        val held = mapOf(0 to "bg", 1 to "bg")
        assertEquals(held, CelExposureIndex.insertContinuingHold(held, 2, 4))
    }

    @Test fun `a frame between two different drawings stays empty`() {
        val held = mapOf(0 to "a", 1 to "b")
        assertEquals(mapOf(0 to "a", 2 to "b"), CelExposureIndex.insertContinuingHold(held, 1, 2))
    }

    @Test fun `a frame added before everything stays empty`() {
        assertEquals(mapOf(1 to "bg", 2 to "bg", 3 to "bg"), CelExposureIndex.insertContinuingHold(heldToEnd, 0, 3))
    }
}
