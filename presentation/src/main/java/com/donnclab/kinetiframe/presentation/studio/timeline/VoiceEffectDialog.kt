package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectAsset
import com.donnclab.kinetiframe.core.document.VoiceEffect
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.choiceLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [render] makes the restyled audio asset for a chosen style; it runs off the main thread. */
@Composable
internal fun VoiceEffectDialog(
    clip: AudioClipPlacement,
    render: suspend (VoiceEffect) -> ProjectAsset,
    onDismiss: () -> Unit,
    onSave: (VoiceEffect, ProjectAsset?) -> Unit,
) {
    var selected by remember { mutableStateOf(clip.voiceEffect) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val processFailed = stringResource(R.string.voice_process_failed)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.voice_title)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            VoiceEffect.entries.forEach { effect ->
                TextButton({ selected = effect }, enabled = !busy) {
                    Text(choiceLabel(stringResource(effect.labelRes()), selected = selected == effect))
                }
            }
            Text(stringResource(R.string.voice_note))
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
                    val asset = withContext(Dispatchers.IO) { render(selected) }
                    onSave(selected, asset)
                } catch (
                    failure: kotlinx.coroutines.CancellationException,
                ) {
                    throw failure
                } catch (
                    failure: Exception,
                ) {
                    error = failure.message ?: processFailed
                } finally {
                    busy = false
                }
            }
        }, enabled = !busy) { Text(stringResource(R.string.action_apply)) }
    }, dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}

@StringRes
private fun VoiceEffect.labelRes(): Int = when (this) {
    VoiceEffect.ORIGINAL -> R.string.voice_original
    VoiceEffect.WARM -> R.string.voice_warm
    VoiceEffect.RADIO -> R.string.voice_radio
    VoiceEffect.ROBOT -> R.string.voice_robot
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun VoiceStylePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        VoiceEffectDialog(
            AudioClipPlacement("clip", "audio", 0, "Voice", 0, 24),
            { error("No audio in a preview") },
            {},
            { _, _ -> },
        )
    }
}
