package com.donnclab.kinetiframe.engine.drawing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushContractTest {

    @Test
    fun `default snapshot remains the backwards compatible pen`() {
        assertEquals(BrushId.PEN, BrushSnapshot().id)
        assertEquals(1, BrushSnapshot().rendererVersion)
    }

    @Test
    fun `launch catalog contains unique stable brush ids`() {
        assertEquals(18, BrushCatalog.drawingBrushes.size)
        assertEquals(BrushCatalog.drawingBrushes.size, BrushCatalog.drawingBrushes.toSet().size)
        assertTrue(BrushCatalog.drawingBrushes.contains(BrushId.PEN))
        assertTrue(
            BrushCatalog.drawingBrushes.containsAll(
                listOf(
                    BrushId.HAIR, BrushId.FUR, BrushId.SKETCH, BrushId.CALLIGRAPHY,
                    BrushId.SPRAY, BrushId.STIPPLE, BrushId.ROUGH_PEN, BrushId.GRAIN_PEN,
                    BrushId.DASH, BrushId.GEAR, BrushId.SAW,
                ),
            ),
        )
    }

    @Test
    fun `brush display names are present for the picker`() {
        assertTrue(BrushCatalog.drawingBrushes.all { it.displayName.isNotBlank() })
    }

    @Test
    fun `particle noise is deterministic, bounded, and channel separated`() {
        val first = BrushDynamics.deterministicUnit(seed = 91L, index = 12, channel = 3)
        val replay = BrushDynamics.deterministicUnit(seed = 91L, index = 12, channel = 3)
        val differentChannel = BrushDynamics.deterministicUnit(seed = 91L, index = 12, channel = 4)

        assertEquals(first, replay, 0f)
        assertTrue(first in 0f..1f)
        assertTrue(BrushDynamics.symmetricNoise(91L, 12, 1) in -1f..1f)
        assertFalse(first == differentChannel)
    }

    @Test
    fun `crayon tooth coverage and micro-grain radius stay bounded`() {
        assertEquals(0.52f, BrushDynamics.crayonClusterCoverage(0f), 0.001f)
        assertEquals(0.87f, BrushDynamics.crayonClusterCoverage(1f), 0.001f)
        assertTrue(BrushDynamics.crayonClusterCoverage(0.4f) in 0f..1f)
        assertEquals(0.35f, BrushDynamics.crayonRadiusMultiplier(0f), 0.001f)
        assertEquals(1.30f, BrushDynamics.crayonRadiusMultiplier(1f), 0.001f)
    }

    @Test
    fun `crayon endpoint taper reduces pooling without erasing the mark`() {
        val atEnd = BrushDynamics.crayonEndpointOpacity(distanceFromEdge = 0f, width = 10f)
        val inBody = BrushDynamics.crayonEndpointOpacity(distanceFromEdge = 20f, width = 10f)

        assertEquals(0.32f, atEnd, 0.001f)
        assertEquals(1f, inBody, 0.001f)
        assertTrue(atEnd in 0f..1f)
    }

    @Test
    fun `ink pressure curve is monotonic and bounded`() {
        val low = BrushDynamics.inkWidthMultiplier(0f)
        val middle = BrushDynamics.inkWidthMultiplier(0.5f)
        val high = BrushDynamics.inkWidthMultiplier(1f)

        assertTrue(low <= middle)
        assertTrue(middle <= high)
        assertEquals(0.62f, low, 0.001f)
        assertEquals(1.5f, high, 0.001f)
    }

    @Test
    fun `opacity recipes remain bounded`() {
        assertEquals(107, BrushDynamics.highlighterAlpha(255))
        assertEquals(28, BrushDynamics.airbrushAlpha(255))
        assertEquals(1, BrushDynamics.airbrushAlpha(0))
    }
}
