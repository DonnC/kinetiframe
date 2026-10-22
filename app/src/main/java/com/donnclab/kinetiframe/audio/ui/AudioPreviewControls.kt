package com.donnclab.kinetiframe.audio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

@Composable
fun AudioPreview(path: String) {
    val model: AudioPreviewViewModel = viewModel(
        key = "asset-audio-preview",
        factory =
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AudioPreviewViewModel(AndroidPreviewAudioPlayer()) as T
        },
    )
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(path) {
        model.handleIntent(AudioPreviewIntent.Load(path))
        onStopOrDispose { model.handleIntent(AudioPreviewIntent.Stop) }
    }
    AudioPreviewControls(state, model::handleIntent)
}

@Composable
fun AudioPreviewControls(state: AudioPreviewState, onIntent: (AudioPreviewIntent) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onIntent(AudioPreviewIntent.Toggle) }, enabled = state.ready) {
                Icon(
                    if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (state.playing) "Pause audio" else "Play audio",
                )
            }
            Slider(
                value = drag ?: state.positionMs.toFloat(),
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { onIntent(AudioPreviewIntent.Seek(it.toInt())) }
                    drag = null
                },
                valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
                enabled = state.ready,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Audio position" },
            )
        }
        Text(
            "${audioTime((drag ?: state.positionMs.toFloat()).toInt())} / ${audioTime(state.durationMs)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
private fun audioTime(ms: Int): String = "%d:%02d".format(ms / 60_000, (ms / 1_000) % 60)

@Preview(showBackground = true)
@Composable
private fun AudioControlsPreview() {
    KinetiFrameTheme { AudioPreviewControls(AudioPreviewState(ready = true, durationMs = 2400), {}) }
}
