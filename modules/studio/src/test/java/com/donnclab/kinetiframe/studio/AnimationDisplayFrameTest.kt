package com.donnclab.kinetiframe.studio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimationDisplayFrameTest {

    @Test
    fun `project presentation uses playback frame without moving editor selection`() {
        val animation = AnimationState(currentFrame = 3, playbackFrame = 17, isPlaying = true)

        assertEquals(17, animation.displayFrame())
        assertEquals(3, animation.currentFrame)
    }

    @Test
    fun `project presentation returns to selected frame once playback stops`() {
        val animation = AnimationState(currentFrame = 3, playbackFrame = 17, isPlaying = false)

        assertEquals(3, animation.displayFrame())
    }
}
