package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AudioTrackSyncTest {
    private fun clip(id: String, track: Int, start: Int, duration: Int, trimStartMicros: Long = 0L) =
        AudioClipPlacement(id, "asset-$id", track, "Clip $id", start, duration, trimStartMicros)

    @Test fun `a frame before or after a clip's range yields no active clip`() {
        val clip = clip("a", 0, 10, 5)
        assertTrue(AudioTrackSync.activeClips(listOf(clip), frame = 9, fps = 12).isEmpty())
        assertTrue(AudioTrackSync.activeClips(listOf(clip), frame = 15, fps = 12).isEmpty())
    }

    @Test fun `a frame inside a clip's range computes the correct offset from its start`() {
        val clip = clip("a", 0, 10, 24)
        // Frame 10 is the clip's own first frame -- zero frames in, so zero offset.
        assertEquals(0L, AudioTrackSync.activeClips(listOf(clip), frame = 10, fps = 12).single().offsetMicros)
        // 6 frames into a 12fps clip is exactly half a second.
        assertEquals(500_000L, AudioTrackSync.activeClips(listOf(clip), frame = 16, fps = 12).single().offsetMicros)
    }

    @Test fun `a clip's own trim in-point shifts every computed offset by that amount`() {
        val clip = clip("a", 0, 0, 24, trimStartMicros = 2_000_000L)
        assertEquals(2_000_000L, AudioTrackSync.activeClips(listOf(clip), frame = 0, fps = 12).single().offsetMicros)
        assertEquals(2_500_000L, AudioTrackSync.activeClips(listOf(clip), frame = 6, fps = 12).single().offsetMicros)
    }

    @Test fun `clips on different tracks that overlap in time are all reported active at once`() {
        val voice = clip("voice", 0, 0, 24)
        val music = clip("music", 1, 0, 48)
        val active = AudioTrackSync.activeClips(listOf(voice, music), frame = 5, fps = 12)
        assertEquals(setOf("voice", "music"), active.map { it.clipId }.toSet())
    }

    @Test fun `a non-positive fps never divides by zero, it simply reports nothing active`() {
        val clip = clip("a", 0, 0, 24)
        assertTrue(AudioTrackSync.activeClips(listOf(clip), frame = 0, fps = 0).isEmpty())
        assertTrue(AudioTrackSync.activeClips(listOf(clip), frame = 0, fps = -5).isEmpty())
    }
}
