package com.donnclab.kinetiframe.video

import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.studio.drawing.Affine2D
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SceneVideoGeometryTest {
    private val clip = VideoClipPlacement(
        "clip", "video", "Screen", 12, 36, isSceneContent = true,
        x = 100f, y = 50f, width = 200f, height = 100f,
    )

    @Test fun `affine transform preserves rotation reflection and translation`() {
        val moved = clip.copy(selectionTransform = Affine2D(0f, 2f, -2f, 0f, 600f, 0f))
        val bounds = SceneVideoGeometry.bounds(moved)
        assertEquals(300f, bounds.left)
        assertEquals(500f, bounds.right)
        assertEquals(200f, bounds.top)
        assertEquals(600f, bounds.bottom)
    }

    @Test fun `scene range source window and placement survive serialization`() {
        val moved = clip.copy(videoSourceOffsetSeconds = 1.25, selectionTransform = Affine2D(tx = 12f))
        assertEquals(moved, Json.decodeFromString<VideoClipPlacement>(Json.encodeToString(moved)))
        val legacy = Json.decodeFromString<VideoClipPlacement>(
            """{"clipId":"old","assetId":"v","label":"Trace","timelineStartFrame":0,"timelineEndFrame":12}""",
        )
        assertFalse(legacy.isSceneContent)
    }

    @Test fun `non finite offset and degenerate dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { clip.copy(width = 0f) }
        assertThrows(IllegalArgumentException::class.java) { clip.copy(videoSourceOffsetSeconds = Double.NaN) }
    }
}
