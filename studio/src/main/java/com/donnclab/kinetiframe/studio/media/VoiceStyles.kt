package com.donnclab.kinetiframe.studio.media

import com.donnclab.kinetiframe.core.document.AssetKind
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.VoiceEffect
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.media.audio.data.VoiceEffectProxy
import java.io.File
import java.util.UUID

/**
 * Voice styles for audio clips. A style is rendered once into a new audio asset in the project's
 * folder; the clip's original audio is kept, so the style can be changed or removed later. Call
 * off the main thread.
 */
class VoiceStyles(private val files: DocumentFileStore) {
    /** Renders [clip]'s original audio in [effect] as a new asset of [project]. */
    suspend fun render(clip: AudioClipPlacement, project: ProjectDocument, effect: VoiceEffect): ProjectAsset {
        val original = requireNotNull(project.assets[clip.originalAssetId ?: clip.assetId]?.sourcePath) {
            "Audio source missing"
        }
        val id = "voice-${UUID.randomUUID()}"
        val output = File(files.projectDirectory(project.id), "project-assets/audio/$id.wav")
        val duration = clip.sourceDurationMicros.takeIf { it > 0 }
            ?: (clip.trimStartMicros + clip.durationFrames * 1_000_000L / project.fps)
        VoiceEffectProxy().create(File(original), duration, effect, output)
        return ProjectAsset(id, AssetKind.AUDIO, output.absolutePath, 0, 0)
    }
}
