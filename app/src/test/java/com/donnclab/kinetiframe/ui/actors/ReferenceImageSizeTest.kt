package com.donnclab.kinetiframe.ui.actors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReferenceImageSizeTest {
    @Test fun `small sources never upscale`() {
        assertEquals(
            320 to 200,
            ReferenceImageSize.target(
                320,
                200,
                256L * 1024 * 1024,
            ),
        )
    }

    @Test fun `low heap square respects byte budget`() {
        val (w, h) = ReferenceImageSize.target(8000, 8000, 64L * 1024 * 1024)
        assertTrue(w.toLong() * h * 4 <= 64L * 1024 * 1024 / 32)
        assertEquals(w, h)
    }

    @Test fun `panorama retains proportions and bounded edge`() {
        val (w, h) = ReferenceImageSize.target(16000, 1000, 512L * 1024 * 1024)
        assertEquals(1536, w)
        assertEquals(96, h)
    }

    @Test fun `invalid sizes fail before decode`() {
        assertThrows(IllegalArgumentException::class.java) { ReferenceImageSize.target(0, 10, 1) }
    }
}
