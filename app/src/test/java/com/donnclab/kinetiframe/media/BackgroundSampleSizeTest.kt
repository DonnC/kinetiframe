package com.donnclab.kinetiframe.media

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackgroundSampleSizeTest {
    @Test fun `ordinary project background keeps native resolution`() {
        assertEquals(1, backgroundSampleSize(1920, 1080))
        assertEquals(2, backgroundSampleSize(3840, 2160))
        assertEquals(4, backgroundSampleSize(8000, 6000))
    }

    @Test fun `large and extremely tall images stay within pixel and edge limits`() {
        for ((w, h) in listOf(12000 to 9000, 1 to Int.MAX_VALUE, Int.MAX_VALUE to 1, Int.MAX_VALUE to Int.MAX_VALUE)) {
            val sample = backgroundSampleSize(w, h)
            assertEquals(0, sample and (sample - 1))
            val width = (w.toLong() + sample - 1) / sample
            val height = (h.toLong() + sample - 1) / sample
            assertTrue(width * height <= 4_000_000)
            assertTrue(width <= 4096 && height <= 4096)
        }
    }

    @Test fun `invalid metadata does not reach the native decoder`() {
        assertThrows(IllegalArgumentException::class.java) { backgroundSampleSize(0, 100) }
        assertThrows(IllegalArgumentException::class.java) { backgroundSampleSize(100, -1) }
        assertThrows(IllegalArgumentException::class.java) { backgroundSampleSize(100, 100, 0) }
    }
}
