package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VideoTrackSyncTest {
    private fun clip(id: String, start: Int, end: Int, offsetSeconds: Double = 0.0) =
        VideoClipPlacement(id, "asset-$id", "Clip $id", start, end, offsetSeconds)

    @Test fun `a frame outside every clip's range has no active clip`() {
        val clip = clip("a", 10, 20)
        assertNull(VideoTrackSync.activeClip(listOf(clip), frame = 9, fps = 12))
        assertNull(VideoTrackSync.activeClip(listOf(clip), frame = 20, fps = 12))
    }

    @Test fun `a frame inside a clip's range computes the correct source offset`() {
        val clip = clip("a", 10, 100, offsetSeconds = 2.0)
        // Frame 10 is the clip's own first frame -- zero frames in, so exactly the in-point.
        assertEquals(2.0, VideoTrackSync.activeClip(listOf(clip), frame = 10, fps = 12)!!.sourceOffsetSeconds, 0.0001)
        // 6 frames into a 12fps clip is half a second further into the source.
        assertEquals(2.5, VideoTrackSync.activeClip(listOf(clip), frame = 16, fps = 12)!!.sourceOffsetSeconds, 0.0001)
    }

    @Test fun `at most one clip is ever reported active, matching the single-track model`() {
        val clips = listOf(clip("a", 0, 10), clip("b", 10, 20))
        assertEquals("a", VideoTrackSync.activeClip(clips, frame = 5, fps = 12)!!.clipId)
        assertEquals("b", VideoTrackSync.activeClip(clips, frame = 15, fps = 12)!!.clipId)
    }

    @Test fun `a non-positive fps never divides by zero, it simply reports nothing active`() {
        val clip = clip("a", 0, 10)
        assertNull(VideoTrackSync.activeClip(listOf(clip), frame = 0, fps = 0))
        assertNull(VideoTrackSync.activeClip(listOf(clip), frame = 0, fps = -5))
    }
}
