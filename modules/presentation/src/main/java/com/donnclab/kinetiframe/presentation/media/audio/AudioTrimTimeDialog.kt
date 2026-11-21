package com.donnclab.kinetiframe.presentation.media.audio

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.media.audio.AudioTrimSelection
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.CompactTextField

/** Exact millisecond entry complements the waveform without permanently adding controls. */
@Composable
internal fun AudioTrimTimeDialog(
    selection: AudioTrimSelection,
    onDismiss: () -> Unit,
    onConfirm: (AudioTrimSelection) -> Unit,
) {
    var start by remember { mutableStateOf((selection.inMicros / 1_000).toString()) }
    var end by remember { mutableStateOf((selection.outMicros / 1_000).toString()) }
    val startMs = start.toLongOrNull()
    val endMs = end.toLongOrNull()
    val maximum = selection.sourceDurationMicros / 1_000
    val valid = startMs != null && endMs != null && startMs in 0..maximum && endMs in 0..maximum &&
        endMs > startMs && endMs - startMs >= minOf(50L, maximum)
    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(stringResource(R.string.audio_exact_trim_title))
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
            CompactTextField(
                start,
                { start = it },
                label = { Text(stringResource(R.string.audio_exact_trim_in)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            CompactTextField(
                end,
                { end = it },
                label = { Text(stringResource(R.string.audio_exact_trim_out)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(stringResource(R.string.audio_exact_trim_source, maximum), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(enabled = valid, onClick = {
            if (valid && startMs != null &&
                endMs != null
            ) {
                onConfirm(selection.copy(inMicros = startMs * 1_000, outMicros = endMs * 1_000))
            }
        }) { Text(stringResource(R.string.action_apply)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}
