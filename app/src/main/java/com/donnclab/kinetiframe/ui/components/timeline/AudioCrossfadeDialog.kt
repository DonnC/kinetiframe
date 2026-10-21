package com.donnclab.kinetiframe.ui.components.timeline

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
import com.donnclab.kinetiframe.engine.model.AudioClipPlacement
import kotlin.math.roundToInt

@Composable
internal fun AudioCrossfadeDialog(
    first: AudioClipPlacement,
    next: AudioClipPlacement,
    fps: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val max = minOf(first.durationFrames, next.durationFrames)
    var frames by remember(first.clipId, next.clipId) { mutableFloatStateOf(minOf(max, maxOf(1, fps / 2)).toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crossfade to ${next.label}") },
        text = {
            Column {
                Text("Overlap · ${"%.2f".format(frames / fps)} s")
                Slider(frames, { frames = it }, valueRange = 1f..max.coerceAtLeast(2).toFloat(), enabled = max > 1)
                Text("The next clip moves earlier to overlap. No speech is sped up or cut.")
            }
        },
        confirmButton = { TextButton({ onSave(frames.roundToInt().coerceAtMost(max)) }) { Text("Apply") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Preview
@Composable
private fun CrossfadePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        AudioCrossfadeDialog(
            AudioClipPlacement(
                "a",
                "s",
                0,
                "First",
                0,
                24,
            ),
            AudioClipPlacement("b", "s", 0, "Second", 24, 24),
            12,
            {
            },
            {},
        )
    }
}
