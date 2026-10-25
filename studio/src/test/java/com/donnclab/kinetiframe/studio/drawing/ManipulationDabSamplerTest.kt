package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManipulationDabSamplerTest {
    private fun samples(points: List<TouchPoint>): List<Float> {
        val sampler = ManipulationDabSampler(40f)
        val result = mutableListOf<Float>()
        points.zipWithNext().forEach { (a, b) -> sampler.segment(a, b) { x, _, _, _, _ -> result.add(x) } }
        return result
    }

    @Test fun `touch event density does not change dab locations`() {
        val sparse = samples(listOf(TouchPoint(0f, 0f), TouchPoint(96f, 0f)))
        val dense = samples((0..24).map { TouchPoint(it * 4f, 0f) })
        assertEquals(sparse.size, dense.size)
        sparse.zip(dense).forEach { (a, b) -> assertEquals(a, b, 0.001f) }
    }

    @Test fun `long fast strokes are not truncated to thirty two steps`() {
        val result = samples(listOf(TouchPoint(0f, 0f), TouchPoint(960f, 0f)))
        assertTrue(result.size > 32)
        assertTrue(result.zipWithNext().all { (a, b) -> b - a < 3f })
    }
}
