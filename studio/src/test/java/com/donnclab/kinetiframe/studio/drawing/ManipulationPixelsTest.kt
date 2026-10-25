package com.donnclab.kinetiframe.studio.drawing

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManipulationPixelsTest {
    private val size = 25
    private val red = 0xffff0000.toInt()
    private fun apply(input: IntArray, blur: Boolean, amount: Float = 1f, dx: Float = 2f): IntArray {
        val result = IntArray(input.size)
        ManipulationPixels().apply(input, result, size, size, 12.5f, 12.5f, 10f, dx, 0f, amount, blur, 2)
        return result
    }

    @Test fun `blur softens alpha on both sides of an edge without black fringes`() {
        val input = IntArray(size * size) { if (it % size < 12) red else 0 }
        val result = apply(input, true)
        val inner = result[12 * size + 11]
        val outer = result[12 * size + 12]
        assertTrue(inner ushr 24 in 1..254)
        assertTrue(outer ushr 24 in 1..254)
        assertEquals(255, outer ushr 16 and 255)
        assertEquals(0, outer and 0xffff)
    }

    @Test fun `uniform semitransparent paint never becomes more opaque`() {
        val input = IntArray(size * size) { 0x80ff0000.toInt() }
        assertArrayEquals(input, apply(input, true))
        assertArrayEquals(input, apply(input, false))
    }

    @Test fun `smudge carries pigment in stroke direction and preserves outside footprint`() {
        val input = IntArray(size * size) { if (it % size < 12) red else 0 }
        val result = apply(input, false)
        assertEquals(red, result[12 * size + 12])
        assertEquals(input[0], result[0])
        assertEquals(input.last(), result.last())
    }

    @Test fun `zero exposure is an exact no op`() {
        val input = IntArray(size * size) { if (it % 2 == 0) red else 0 }
        assertArrayEquals(input, apply(input, true, amount = 0f))
        assertArrayEquals(input, apply(input, false, amount = 0f))
    }

    @Test fun `blur is symmetric around an isolated pixel`() {
        val input = IntArray(size * size)
        input[12 * size + 12] = -1
        val result = apply(input, true)
        for (offset in 1..6) assertEquals(result[12 * size + 12 - offset], result[12 * size + 12 + offset])
    }
}
