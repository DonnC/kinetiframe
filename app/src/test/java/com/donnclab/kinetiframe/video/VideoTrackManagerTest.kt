package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoTrackManagerTest {
    private var state = StudioState()
    private val manager = VideoTrackManager({ state }, { state = it(state) })

    private fun asset(id: String) =
        ProjectAsset(id = id, kind = AssetKind.VIDEO, sourcePath = "/video/$id.mp4", width = 0, height = 0)
    private fun clip(clipId: String, assetId: String, start: Int, end: Int, offset: Double = 0.0) =
        VideoClipPlacement(clipId, assetId, "Clip", start, end, offset)

    @Test fun `adding a clip inserts both the placement and its backing asset together`() {
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        assertEquals(1, state.project.videoClips.size)
        assertTrue("asset-a" in state.project.assets)
    }

    @Test fun `adding a clip whose id already exists throws rather than silently overwriting`() {
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        assertThrows(IllegalArgumentException::class.java) {
            manager.add(clip("clip-a", "asset-b", 20, 30), asset("asset-b"))
        }
    }

    @Test fun `adding a clip whose assetId disagrees with the provided asset throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-b"))
        }
    }

    @Test fun `adding a clip that overlaps an existing one on the single track throws`() {
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        assertThrows(IllegalArgumentException::class.java) {
            manager.add(clip("clip-b", "asset-b", 5, 15), asset("asset-b"))
        }
        // Exactly adjacent (touching, not overlapping) is fine.
        manager.add(clip("clip-c", "asset-c", 10, 20), asset("asset-c"))
        assertEquals(2, state.project.videoClips.size)
    }

    @Test fun `trimming into a range that would overlap a neighbor is silently ignored`() {
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        manager.add(clip("clip-b", "asset-b", 10, 20), asset("asset-b"))
        manager.trim("clip-a", 0, 15, 0.0) // would now overlap clip-b
        assertEquals(10, state.project.videoClips.getValue("clip-a").timelineEndFrame)
    }

    @Test fun `trimming updates the range and offset when it stays non-overlapping`() {
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        val offset = 2.0 / state.project.fps
        manager.trim("clip-a", 2, 8, offset)
        val clip = state.project.videoClips.getValue("clip-a")
        assertEquals(2, clip.timelineStartFrame)
        assertEquals(8, clip.timelineEndFrame)
        assertEquals(offset, clip.videoSourceOffsetSeconds, 0.0001)
    }

    @Test fun `widening a trim restores the original source window`() {
        manager.add(clip("clip-a", "asset-a", 2, 14, 2.0), asset("asset-a"))
        manager.trim("clip-a", 4, 8, 2.0 + 2.0 / state.project.fps)
        manager.trim("clip-a", 2, 14, 2.0)
        val restored = state.project.videoClips.getValue("clip-a")
        assertEquals(2, restored.timelineStartFrame)
        assertEquals(14, restored.timelineEndFrame)
        assertEquals(2.0, restored.videoSourceOffsetSeconds, .00001)
    }

    @Test fun `nextFreeStartFrame is 0 when empty and the tail end otherwise`() {
        assertEquals(0, manager.nextFreeStartFrame())
        manager.add(clip("clip-a", "asset-a", 0, 10), asset("asset-a"))
        assertEquals(10, manager.nextFreeStartFrame())
    }

    @Test fun `setTrackOpacity clamps into 0 to 1`() {
        manager.setTrackOpacity(4f)
        assertEquals(1f, state.project.videoTrackOpacity)
        manager.setTrackOpacity(-2f)
        assertEquals(0f, state.project.videoTrackOpacity)
    }

    @Test fun `scene video extends timeline and moving preserves its stage transform`() {
        val video = clip("scene", "asset", 2, 30).copy(
            isSceneContent = true,
            x = 100f,
            y = 40f,
            width = 320f,
            height = 180f,
            selectionTransform = com.donnclab.kinetiframe.core.geometry.Affine2D(tx = 24f),
        )
        manager.add(video, asset("asset"))
        assertTrue(state.animation.totalFrames >= 30)
        manager.trim("scene", 60, 88, 0.0)
        val moved = state.project.videoClips.getValue("scene")
        assertEquals(video.selectionTransform, moved.selectionTransform)
        assertEquals(320f, moved.width)
        assertTrue(moved.isSceneContent)
        assertTrue(state.animation.totalFrames >= 88)
    }
}
