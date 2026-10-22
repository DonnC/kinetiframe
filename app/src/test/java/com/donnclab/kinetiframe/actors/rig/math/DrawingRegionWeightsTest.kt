package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.actors.rig.model.ActorDrawingRegion
import com.donnclab.kinetiframe.actors.rig.model.DrawingPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrawingRegionWeightsTest {
    private val region =
        ActorDrawingRegion(
            "arm",
            listOf(DrawingPoint(.1f, .1f), DrawingPoint(.9f, .1f), DrawingPoint(.9f, .9f), DrawingPoint(.1f, .9f)),
            10f,
        )

    @Test fun `inside edge feathers smoothly and outside never changes weights`() {
        assertEquals(0f, DrawingRegionWeights.influence(region, 0f, 50f, 100, 100))
        assertEquals(.5f, DrawingRegionWeights.influence(region, 15f, 50f, 100, 100), .001f)
        assertEquals(1f, DrawingRegionWeights.influence(region, 50f, 50f, 100, 100))
    }

    @Test fun `adding a fifth influence prunes and renormalizes instead of exceeding renderer budget`() {
        val actual = DrawingRegionWeights.blend(listOf(.25f, .25f, .25f, .25f, 0f), 4, .8f)
        assertEquals(4, actual.count { it > 0f })
        assertEquals(1f, actual.sum(), .0001f)
        assertTrue(actual[4] > .8f)
    }
}
