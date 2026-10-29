package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.donnclab.kinetiframe.presentation.media.audio.AudioPreview

/** Inspect the actual sound before inserting it. Preview does not copy or mutate the asset. */
@Composable
fun LibraryAudioPreviewDialog(preview: AudioItemPreview, onDismiss: () -> Unit, onInsert: () -> Unit) {
    val item = preview.item
    val path = preview.path
    val error = preview.error
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                if (path != null) {
                    AudioPreview(path)
                } else if (error == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    TextButton(onClick = onInsert, enabled = path != null) { Text("Insert") }
                }
            }
        }
    }
}
