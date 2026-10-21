package com.donnclab.kinetiframe.engine.drawing

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SketchColourFillTest {
    @Test fun `closed contour contains colour without changing original ink`() {
        val ink = IntArray(25) { i -> if (i / 5 in 1..3 && i % 5 in 1..3 && (i / 5 != 2 || i % 5 != 2)) -1 else 0 }
        val original = ink.copyOf()
        val result = SketchColourFill.apply(ink, IntArray(25), 5, 5, 2, 2, 0xff00ff00.toInt())
        assertEquals(1, result.count { it != 0 })
        assertEquals(0xff00ff00.toInt(), result[12])
        assertArrayEquals(original, ink)
    }

    @Test fun `recolour only replaces connected matching pixels`() {
        val pixels = intArrayOf(1, 1, 2, 1, 1, 2)
        assertArrayEquals(intArrayOf(3, 3, 2, 3, 3, 2), SketchColourFill.apply(IntArray(6), pixels, 3, 2, 0, 0, 3))
        assertArrayEquals(intArrayOf(1, 1, 2, 1, 1, 2), pixels)
    }

    @Test fun `open contour fills canvas and out of bounds is a no op`() {
        assertTrue(SketchColourFill.apply(IntArray(9), IntArray(9), 3, 3, 1, 1, 4).all { it == 4 })
        assertTrue(SketchColourFill.apply(IntArray(9), IntArray(9), 3, 3, -1, 0, 4).all { it == 0 })
    }
}
