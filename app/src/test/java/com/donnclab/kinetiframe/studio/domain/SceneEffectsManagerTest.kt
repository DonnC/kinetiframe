package com.donnclab.kinetiframe.studio.domain

import com.donnclab.kinetiframe.project.model.AudioClipPlacement
import com.donnclab.kinetiframe.project.model.AudioTrackSettings
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.VideoClipPlacement
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.camera.CameraAction
import com.donnclab.kinetiframe.studio.camera.CameraShot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SceneEffectsManagerTest {
    private var state = StudioState()
    private val manager = SceneEffectsManager({ state }, { state = it(state) })

    @Test fun `camera edits undo redo without replacing viewport`() {
        val shot = CameraShot("pan", CameraAction.PAN, 0, 10)
        manager.put(shot)
        state = state.copy(canvas = state.canvas.copy(zoom = 2f))
        manager.undo()
        assertTrue(state.project.cameraShots.isEmpty())
        assertEquals(2f, state.canvas.zoom)
        manager.redo()
        assertEquals(listOf(shot), state.project.cameraShots)
        manager.remove("pan")
        assertTrue(state.project.cameraShots.isEmpty())
    }

    @Test fun `failed overlap does not modify history or project`() {
        manager.put(CameraShot("a", CameraAction.PAN, 0, 10))
        val old = state
        assertThrows(IllegalArgumentException::class.java) { manager.put(CameraShot("b", CameraAction.PAN, 5, 20)) }
        assertEquals(old, state)
        manager.undo()
        assertTrue(state.project.cameraShots.isEmpty())
    }

    @Test fun `video split preserves source and placement and can be undone`() {
        val clip = VideoClipPlacement(
            "v", "asset", "Video", 10, 40, videoSourceOffsetSeconds = 2.0,
            isSceneContent = true, x = 12f, y = 15f, width = 100f, height = 80f,
        )
        state = state.copy(project = state.project.copy(fps = 24, videoClips = mapOf("v" to clip)))
        manager.splitVideo("v", 22)
        val left = state.project.videoClips.getValue("v")
        val right = state.project.videoClips.values.single { it.clipId != "v" }
        assertEquals(22, left.timelineEndFrame)
        assertEquals(22, right.timelineStartFrame)
        assertEquals(2.5, right.videoSourceOffsetSeconds, .00001)
        assertEquals(clip.assetId, right.assetId)
        assertEquals(clip.x, right.x)
        assertEquals(12, right.sourceOffsetFrames)
        manager.undo()
        assertEquals(mapOf("v" to clip), state.project.videoClips)
        manager.redo()
        assertEquals(2, state.project.videoClips.size)
    }

    @Test fun `fade limits and locked lanes are respected`() {
        val clip = AudioClipPlacement("a", "asset", 0, "Sound", 0, 24)
        state = state.copy(project = state.project.copy(audioClips = mapOf("a" to clip)))
        manager.fades("a", 100, -1)
        assertEquals(24, state.project.audioClips.getValue("a").fadeInFrames)
        assertEquals(0, state.project.audioClips.getValue("a").fadeOutFrames)
        state = state.copy(project = state.project.copy(audioTracks = mapOf(0 to AudioTrackSettings(isLocked = true))))
        manager.fades("a", 0, 24)
        assertEquals(24, state.project.audioClips.getValue("a").fadeInFrames)
    }

    @Test fun `history never crosses project boundaries`() {
        manager.put(CameraShot("a", CameraAction.PAN, 0, 10))
        state = state.copy(project = ProjectDocument(id = "new"))
        manager.undo()
        assertTrue(state.project.cameraShots.isEmpty())
    }
}
