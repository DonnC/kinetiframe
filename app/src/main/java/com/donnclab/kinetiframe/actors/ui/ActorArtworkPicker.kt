package com.donnclab.kinetiframe.actors.ui

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.actors.data.ActorArtworkDecoder
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackArtworkRegistration
import com.donnclab.kinetiframe.library.data.PackItemEntry
import com.donnclab.kinetiframe.library.ui.LibraryPickerDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Shared library browser, specialised only at the final item-to-actor attachment boundary. */
@Composable
fun ActorArtworkPicker(
    library: LibraryRepository,
    temporaryDirectory: File,
    onDismiss: () -> Unit,
    onSelected: (String, Bitmap, PackArtworkRegistration?) -> Unit,
) {
    var pack by remember { mutableStateOf<LibraryItemManifest?>(null) }
    var items by remember { mutableStateOf<List<PackItemEntry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun read(name: String, registration: PackArtworkRegistration? = null, copy: (File) -> StoreResult<*>) {
        if (busy) return
        busy = true
        scope.launch {
            var pending: Bitmap? = null
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val target = File.createTempFile("actor-artwork-", ".png", temporaryDirectory)
                    try {
                        when (val result = copy(target)) {
                            is StoreResult.Failure -> throw IllegalStateException(result.message)

                            is StoreResult.Value -> ActorArtworkDecoder.decode(
                                target,
                                preservePadding =
                                registration != null,
                            ).also { pending = it }
                        }
                    } finally {
                        target.delete()
                    }
                }
                onSelected(name, bitmap, registration)
                pending = null // The attachment flow now owns the pixels.
            } catch (
                cancel: CancellationException,
            ) {
                throw cancel
            } catch (
                failure: Exception,
            ) {
                error = failure.message ?: "Could not read artwork"
            } finally {
                pending?.takeUnless { it.isRecycled }?.recycle()
                busy = false
            }
        }
    }
    val selectedPack = pack
    if (selectedPack == null) {
        LibraryPickerDialog(
            library,
            allowedKinds = setOf(LibraryItemKind.FRAME, LibraryItemKind.PACK),
            dismissOnOpenPack = false,
            onDismiss = onDismiss,
            onOpenPack = { pack = it },
            onPick = { item ->
                read(item.displayName) { destination ->
                    library.copyAssetTo(item.libraryItemId, destination, item.scope, item.sourceProjectId)
                }
            },
        )
    } else {
        LaunchedEffect(selectedPack) {
            when (
                val result =
                    withContext(Dispatchers.IO) {
                        library.loadPack(selectedPack.libraryItemId, selectedPack.scope, selectedPack.sourceProjectId)
                    }
            ) {
                is StoreResult.Value -> items = result.value.second.items
                is StoreResult.Failure -> error = result.message
            }
        }
        Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(12.dp)) {
                    Row {
                        IconButton(onClick = {
                            pack = null
                            items = emptyList()
                        }, enabled = !busy) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to packs")
                        }
                        Text(selectedPack.displayName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = onDismiss, enabled = !busy) {
                            Icon(Icons.Default.Close, "Cancel artwork selection")
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    LazyVerticalGrid(
                        GridCells.Adaptive(88.dp),
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        items(items, key = { it.file }) { item ->
                            Column(
                                Modifier.padding(4.dp).clickable(enabled = !busy) {
                                    read(item.name, item.registration) { destination ->
                                        library.copyPackItemTo(
                                            selectedPack.libraryItemId,
                                            item.file,
                                            destination,
                                            selectedPack.scope,
                                            selectedPack.sourceProjectId,
                                        )
                                    }
                                },
                            ) {
                                AsyncImage(
                                    library.packItemFile(
                                        selectedPack.libraryItemId,
                                        item.file,
                                        selectedPack.scope,
                                        selectedPack.sourceProjectId,
                                    ),
                                    item.name,
                                    Modifier.fillMaxWidth().height(72.dp),
                                )
                                Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}
