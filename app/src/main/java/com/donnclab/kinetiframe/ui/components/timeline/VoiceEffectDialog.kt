package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.donnclab.kinetiframe.engine.audio.VoiceEffect
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun VoiceEffectDialog(
    clip: AudioClipPlacement,
    project: ProjectDocument,
    onDismiss: () -> Unit,
    onSave: (VoiceEffect, ProjectAsset?) -> Unit,
) {
    var selected by remember { mutableStateOf(clip.voiceEffect) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Voice style") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            VoiceEffect.entries.forEach { effect ->
                TextButton({ selected = effect }, enabled = !busy) {
                    Text(
                        (
                            if (selected ==
                                effect
                            ) {
                                "• "
                            } else {
                                ""
                            }
                            ) + effect.name.lowercase().replaceFirstChar { it.uppercase() },
                    )
                }
            }
            Text("Changes the sound, not the speech timing. Original audio is retained.")
            if (busy) LinearProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton({
            if (selected == VoiceEffect.ORIGINAL) {
                onSave(selected, null)
                return@TextButton
            }
            busy = true
            error = null
            scope.launch {
                try {
                    val asset = withContext(Dispatchers.IO) {
                        val original =
                            requireNotNull(project.assets[clip.originalAssetId ?: clip.assetId]?.sourcePath) {
                                "Audio source missing"
                            }
                        val id = "voice-${java.util.UUID.randomUUID()}"
                        val directory = com.donnclab.kinetiframe.data.actor.ActorFileStore(
                            File(context.filesDir, "actor-studio"),
                        ).projectDirectory(project.id)
                        val output = File(directory, "project-assets/audio/$id.wav")
                        val duration =
                            clip.sourceDurationMicros.takeIf { it > 0 }
                                ?: (clip.trimStartMicros + clip.durationFrames * 1_000_000L / project.fps)
                        com.donnclab.kinetiframe.data.audio.VoiceEffectProxy().create(
                            File(original),
                            duration,
                            selected,
                            output,
                        )
                        ProjectAsset(id, AssetKind.AUDIO, output.absolutePath, 0, 0)
                    }
                    onSave(selected, asset)
                } catch (
                    failure: kotlinx.coroutines.CancellationException,
                ) {
                    throw failure
                } catch (
                    failure: Exception,
                ) {
                    error = failure.message ?: "Could not process audio"
                } finally {
                    busy = false
                }
            }
        }, enabled = !busy) { Text("Apply") }
    }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun VoiceStylePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        VoiceEffectDialog(AudioClipPlacement("clip", "audio", 0, "Voice", 0, 24), ProjectDocument(), {}, { _, _ -> })
    }
}
