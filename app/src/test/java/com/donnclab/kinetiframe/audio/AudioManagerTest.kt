package com.donnclab.kinetiframe.audio

import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AudioManagerTest {
    private var state = StudioState()
    private val manager = AudioManager({ state }, { state = it(state) })

    private fun asset(id: String) =
        ProjectAsset(id = id, kind = AssetKind.AUDIO, sourcePath = "/audio/$id.m4a", width = 0, height = 0)
    private fun clip(
        clipId: String,
        assetId: String,
        trackIndex: Int = 0,
        startFrame: Int = 0,
        durationFrames: Int = 24,
    ) = AudioClipPlacement(clipId, assetId, trackIndex, "Clip", startFrame, durationFrames)

    @Test fun `adding a clip inserts both the placement and its backing asset together`() {
        manager.add(clip("clip-a", "asset-a"), asset("asset-a"))
        assertEquals(1, state.project.audioClips.size)
        assertTrue("asset-a" in state.project.assets)
        assertEquals("asset-a", state.project.audioClips.getValue("clip-a").assetId)
    }

    @Test fun `adding a clip whose id already exists throws rather than silently overwriting`() {
        manager.add(clip("clip-a", "asset-a"), asset("asset-a"))
        assertThrows(IllegalArgumentException::class.java) { manager.add(clip("clip-a", "asset-b"), asset("asset-b")) }
    }

    @Test fun `adding a clip whose assetId disagrees with the provided asset throws`() {
        assertThrows(IllegalArgumentException::class.java) { manager.add(clip("clip-a", "asset-a"), asset("asset-b")) }
    }

    @Test fun `removing a clip drops the placement but keeps its asset entry`() {
        manager.add(clip("clip-a", "asset-a"), asset("asset-a"))
        manager.remove("clip-a")
        assertTrue(state.project.audioClips.isEmpty())
        assertTrue("asset-a" in state.project.assets)
    }

    @Test fun `trimming updates start and duration, clamped defensively`() {
        manager.add(clip("clip-a", "asset-a", startFrame = 4, durationFrames = 10), asset("asset-a"))
        manager.trim("clip-a", -3, 0)
        val clip = state.project.audioClips.getValue("clip-a")
        assertEquals(4, clip.startFrame) // Trimming cannot move the block before its source window.
        assertEquals(1, clip.durationFrames)
    }

    @Test fun `trimming an unknown clip id is a no-op`() {
        manager.trim("missing", 3, 4)
        assertTrue(state.project.audioClips.isEmpty())
    }

    @Test fun `moving repositions start frame and track, clamped into range`() {
        manager.add(clip("clip-a", "asset-a", trackIndex = 2, startFrame = 5), asset("asset-a"))
        manager.move("clip-a", 99, -10)
        val clip = state.project.audioClips.getValue("clip-a")
        assertEquals(0, clip.startFrame)
        assertEquals(5, clip.trackIndex) // clamped to MAX_AUDIO_TRACKS - 1
    }

    @Test fun `setVolume clamps into 0 to 1`() {
        manager.add(clip("clip-a", "asset-a"), asset("asset-a"))
        manager.setVolume("clip-a", 4f)
        assertEquals(1f, state.project.audioClips.getValue("clip-a").volume)
        manager.setVolume("clip-a", -2f)
        assertEquals(0f, state.project.audioClips.getValue("clip-a").volume)
    }

    @Test fun `multiple clips can coexist on different tracks`() {
        manager.add(clip("clip-a", "asset-a", trackIndex = 0), asset("asset-a"))
        manager.add(clip("clip-b", "asset-b", trackIndex = 1), asset("asset-b"))
        assertEquals(2, state.project.audioClips.size)
        assertEquals(setOf(0, 1), state.project.audioClips.values.map { it.trackIndex }.toSet())
    }
}
