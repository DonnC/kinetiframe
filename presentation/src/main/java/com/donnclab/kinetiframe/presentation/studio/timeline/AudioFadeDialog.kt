package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import kotlin.math.roundToInt

@Composable
internal fun AudioFadeDialog(clip: AudioClipPlacement, fps: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var incoming by remember(clip.clipId) { mutableFloatStateOf(clip.fadeInFrames.toFloat()) }
    var outgoing by remember(clip.clipId) { mutableFloatStateOf(clip.fadeOutFrames.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio fades") },
        text = {
            Column {
                Text("Fade in · ${"%.2f".format(incoming / fps)} s")
                Slider(incoming, { incoming = it }, valueRange = 0f..clip.durationFrames.coerceAtLeast(1).toFloat())
                Text("Fade out · ${"%.2f".format(outgoing / fps)} s")
                Slider(outgoing, { outgoing = it }, valueRange = 0f..clip.durationFrames.coerceAtLeast(1).toFloat())
            }
        },
        confirmButton = { TextButton({ onSave(incoming.roundToInt(), outgoing.roundToInt()) }) { Text("Apply") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun AudioFadePreview() {
    KinetiFrameTheme { AudioFadeDialog(AudioClipPlacement("clip", "audio", 0, "Voice", 0, 24), 24, {}, { _, _ -> }) }
}
