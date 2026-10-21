package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CutoutPartGuideTest {
    @Test
    fun `guide retains its movable node boundaries and keeps a bounded overlap allowance`() {
        val guide = CutoutPartGuideFactory.create(
            partId = "left_forearm",
            start = Offset(120f, 180f),
            end = Offset(320f, 180f),
        )

        assertEquals(Offset(120f, 180f), guide.start)
        assertEquals(Offset(320f, 180f), guide.end)
        assertTrue(guide.envelopeRadius in 22f..48f)
        assertTrue(guide.overlapRadius < guide.envelopeRadius)
    }

    @Test
    fun `long parts cap their drawing envelope instead of obscuring the complete rig`() {
        val guide = CutoutPartGuideFactory.create("tail", Offset.Zero, Offset(2_000f, 0f))

        assertEquals(48f, guide.envelopeRadius)
    }
}
