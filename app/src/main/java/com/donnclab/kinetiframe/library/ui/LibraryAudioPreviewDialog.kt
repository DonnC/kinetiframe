package com.donnclab.kinetiframe.library.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.media.audio.ui.AudioPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Inspect the actual sound before inserting it. Preview does not copy or mutate the asset. */
@Composable
fun LibraryAudioPreviewDialog(
    item: LibraryItemManifest,
    library: LibraryRepository,
    onDismiss: () -> Unit,
    onInsert: () -> Unit,
) {
    var path by remember(item) { mutableStateOf<String?>(null) }
    var error by remember(item) { mutableStateOf<String?>(null) }
    LaunchedEffect(item) {
        when (val result = withContext(Dispatchers.IO) { library.assetFile(item) }) {
            is StoreResult.Value -> path = result.value.absolutePath
            is StoreResult.Failure -> error = result.message
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                val audioPath = path
                if (audioPath != null) {
                    AudioPreview(audioPath)
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
