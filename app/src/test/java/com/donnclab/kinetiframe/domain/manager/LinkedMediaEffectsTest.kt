package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.audio.VoiceEffect
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.engine.model.AudioTrackSettings
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import com.donnclab.kinetiframe.ui.TimelineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LinkedMediaEffectsTest {
    private val audio = AudioClipPlacement("a", "source", 0, "Sound", 0, 48, fadeOutFrames = 12)
    private val video =
        VideoClipPlacement("v", "source", "Video", 0, 48, sourceDurationFrames = 48, linkedAudioClipId = "a")
    private val source = ProjectAsset("source", AssetKind.VIDEO, "video.mp4", 100, 100)
    private var state = TimelineState(project = ProjectDocument(fps = 12))
    private val videos = VideoTrackManager({ state }, { state = it(state) })
    private val effects = SceneEffectsManager({ state }, { state = it(state) })

    @Test fun `import move trim split and undo keep picture and sound together`() {
        videos.add(video, source, audio)
        assertEquals(audio, state.project.audioClips["a"])
        videos.trim("v", 10, 34, 1.0)
        val trimmed = state.project.audioClips.getValue("a")
        assertEquals(10, trimmed.startFrame)
        assertEquals(24, trimmed.durationFrames)
        assertEquals(1_000_000L, trimmed.trimStartMicros)
        effects.splitVideo("v", 22)
        val right = state.project.videoClips.values.single { it.clipId != "v" }
        val rightAudio = state.project.audioClips.getValue(requireNotNull(right.linkedAudioClipId))
        assertEquals(22, rightAudio.startFrame)
        assertEquals(2_000_000L, rightAudio.trimStartMicros)
        assertEquals(24, rightAudio.envelopeOffsetFrames)
        effects.undo()
        assertEquals(1, state.project.videoClips.size)
        assertEquals(mapOf("a" to trimmed), state.project.audioClips)
        effects.redo()
        assertEquals(2, state.project.audioClips.size)
    }

    @Test fun `locked source sound protects linked timing and removing detached video keeps sound`() {
        videos.add(video, source, audio)
        state = state.copy(project = state.project.copy(audioTracks = mapOf(0 to AudioTrackSettings(isLocked = true))))
        videos.trim("v", 4, 20, 1.0)
        videos.remove("v")
        assertEquals(video, state.project.videoClips["v"])
        videos.detachAudio("v")
        videos.remove("v")
        assertTrue(state.project.videoClips.isEmpty())
        assertEquals(audio, state.project.audioClips["a"])
    }

    @Test fun `voice proxy can be reverted and undone without losing the original asset`() {
        videos.add(video, source, audio)
        val proxy = ProjectAsset("processed", AssetKind.AUDIO, "voice.wav", 0, 0)
        effects.voiceStyle("a", "source", VoiceEffect.ROBOT, proxy)
        assertEquals("processed", state.project.audioClips.getValue("a").assetId)
        assertEquals("source", state.project.audioClips.getValue("a").originalAssetId)
        effects.voiceStyle("a", "processed", VoiceEffect.ORIGINAL, null)
        assertEquals("source", state.project.audioClips.getValue("a").assetId)
        effects.undo()
        assertEquals("processed", state.project.audioClips.getValue("a").assetId)
        assertEquals(source, state.project.assets["source"])
    }

    @Test fun `stale processing result cannot overwrite a newer sound edit`() {
        videos.add(video, source, audio)
        effects.voiceStyle(
            "a",
            "not-current",
            VoiceEffect.ROBOT,
            ProjectAsset("proxy", AssetKind.AUDIO, "voice.wav", 0, 0),
        )
        assertEquals(audio, state.project.audioClips["a"])
    }

    @Test fun `linked audio cannot drift through an independent audio lane drag`() {
        videos.add(video, source, audio)
        val manager = AudioManager({ state }, { state = it(state) })
        manager.move("a", 1, 20)
        manager.trim("a", 4, 10)
        manager.split("a", 12)
        manager.remove("a")
        assertEquals(mapOf("a" to audio), state.project.audioClips)
        videos.detachAudio("v")
        manager.move("a", 1, 20)
        assertEquals(20, state.project.audioClips.getValue("a").startFrame)
    }
}
