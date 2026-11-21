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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R
import kotlin.math.roundToInt

@Composable
internal fun AudioFadeDialog(clip: AudioClipPlacement, fps: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var incoming by remember(clip.clipId) { mutableFloatStateOf(clip.fadeInFrames.toFloat()) }
    var outgoing by remember(clip.clipId) { mutableFloatStateOf(clip.fadeOutFrames.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_fades_title)) },
        text = {
            Column {
                Text(stringResource(R.string.audio_fade_in_seconds, incoming / fps))
                Slider(incoming, { incoming = it }, valueRange = 0f..clip.durationFrames.coerceAtLeast(1).toFloat())
                Text(stringResource(R.string.audio_fade_out_seconds, outgoing / fps))
                Slider(outgoing, { outgoing = it }, valueRange = 0f..clip.durationFrames.coerceAtLeast(1).toFloat())
            }
        },
        confirmButton = {
            TextButton({ onSave(incoming.roundToInt(), outgoing.roundToInt()) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview
@Composable
private fun AudioFadePreview() {
    KinetiFrameTheme { AudioFadeDialog(AudioClipPlacement("clip", "audio", 0, "Voice", 0, 24), 24, {}, { _, _ -> }) }
}
