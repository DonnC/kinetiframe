package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.document.MAX_AUDIO_TRACKS
import com.donnclab.kinetiframe.presentation.common.CompactTextField

/** Frame numbers are one-based in the UI, zero-based at the intent boundary. */
@Composable
internal fun ClipPositionDialog(
    label: String,
    start: Int,
    track: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var frameText by remember { mutableStateOf((start + 1).toString()) }
    var trackText by remember { mutableStateOf(((track ?: 0) + 1).toString()) }
    val frame = frameText.toIntOrNull()?.takeIf { it in 1..1_000_000 }
    val lane = trackText.toIntOrNull()?.takeIf { it in 1..MAX_AUDIO_TRACKS }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Position · $label") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTextField(
                frameText,
                { frameText = it },
                label = { Text("Start frame") },
                singleLine = true,
                isError = frame == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (track !=
                null
            ) {
                CompactTextField(
                    trackText,
                    {
                        trackText = it
                    },
                    label = { Text("Audio track (1–$MAX_AUDIO_TRACKS)") },
                    singleLine = true,
                    isError = lane == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Text(
                "Moves the clip without changing its source in-point or duration.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }, confirmButton = {
        TextButton(enabled = frame != null && lane != null, onClick = {
            if (frame != null && lane != null) onConfirm(frame - 1, lane - 1)
        }) { Text("Move") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Preview
@Composable
private fun ClipPositionPreview() {
    MaterialTheme { ClipPositionDialog("Dialogue", 12, 0, {}, { _, _ -> }) }
}
