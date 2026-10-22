package com.donnclab.kinetiframe.media

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FrameImageDecoderTest {
    @Test fun `camera images are sampled before allocating pixels`() {
        assertEquals(4, FrameImageDecoder.sampleSize(8000, 6000, 2048))
        assertEquals(1, FrameImageDecoder.sampleSize(1280, 720, 2048))
        assertEquals(8, FrameImageDecoder.sampleSize(1000, 12000, 2048))
    }

    @Test fun `invalid dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { FrameImageDecoder.sampleSize(0, 100, 2048) }
    }
}
