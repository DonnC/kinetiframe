package com.donnclab.kinetiframe.audio.ui

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
import androidx.compose.runtime.toString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.audio.AudioTrimSelection
import com.donnclab.kinetiframe.common.ui.CompactTextField

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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Exact source trim") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTextField(
                start,
                { start = it },
                label = { Text("In (milliseconds)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            CompactTextField(
                end,
                { end = it },
                label = { Text("Out (milliseconds)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text("Source: $maximum ms", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(enabled = valid, onClick = {
            if (valid && startMs != null &&
                endMs != null
            ) {
                onConfirm(selection.copy(inMicros = startMs * 1_000, outMicros = endMs * 1_000))
            }
        }) { Text("Apply") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
