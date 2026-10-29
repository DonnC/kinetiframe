package com.donnclab.kinetiframe.presentation.studio.actorstudio

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.library.model.PackArtworkRegistration
import com.donnclab.kinetiframe.presentation.library.LibraryPickerDialog
import com.donnclab.kinetiframe.ui.appViewModel

/** Shared library browser, specialised only at the final item-to-actor attachment boundary. */
@Composable
fun ActorArtworkPicker(onDismiss: () -> Unit, onSelected: (String, Bitmap, PackArtworkRegistration?) -> Unit) {
    val model: ActorArtworkPickerViewModel = appViewModel(key = "actor-artwork-picker")
    val state by model.state.collectAsStateWithLifecycle()
    val selected by rememberUpdatedState(onSelected)
    LaunchedEffect(model) { model.handleIntent(ActorArtworkPickerIntent.Open) }
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                is ActorArtworkPickerEffect.Selected -> selected(effect.name, effect.bitmap, effect.registration)
            }
        }
    }
    val busy = state.busy
    val selectedPack = state.pack
    if (selectedPack == null) {
        LibraryPickerDialog(
            allowedKinds = setOf(LibraryItemKind.FRAME, LibraryItemKind.PACK),
            dismissOnOpenPack = false,
            onDismiss = onDismiss,
            onOpenPack = { model.handleIntent(ActorArtworkPickerIntent.OpenPack(it)) },
            onPick = { model.handleIntent(ActorArtworkPickerIntent.PickItem(it)) },
        )
    } else {
        Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(12.dp)) {
                    Row {
                        IconButton(
                            onClick = { model.handleIntent(ActorArtworkPickerIntent.BackToLibrary) },
                            enabled = !busy,
                        ) {
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
                        items(state.items, key = { it.file }) { item ->
                            Column(
                                Modifier.padding(4.dp).clickable(enabled = !busy) {
                                    model.handleIntent(ActorArtworkPickerIntent.PickPackItem(item))
                                },
                            ) {
                                AsyncImage(model.packItemFile(item), item.name, Modifier.fillMaxWidth().height(72.dp))
                                Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { model.handleIntent(ActorArtworkPickerIntent.DismissError) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { model.handleIntent(ActorArtworkPickerIntent.DismissError) }) { Text("OK") }
            },
        )
    }
}
