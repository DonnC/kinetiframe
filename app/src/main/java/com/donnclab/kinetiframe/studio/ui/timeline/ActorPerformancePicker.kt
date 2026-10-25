package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * The single "+" entry point for adding anything to the timeline -- actor performances, audio,
 * video -- smart-routed to the right track, so there is one Add menu, not one per asset type.
 * Animators only have to find and remember one place to add media.
 *
 * Root menu: Add Actor Performance / Import Audio / Record Audio / Import Video. Choosing
 * "Add Actor Performance" drills into a two-step actor -> performance chooser; the audio/video items are plain actions
 * that hand off to whatever picker/dialog [EditorScreen] wires up for them -- this
 * component never touches Android media APIs itself.
 */
@Composable
fun ActorPerformancePicker(
    projectId: String,
    fps: Int,
    currentFrame: Int,
    actors: ActorRepository,
    performances: PerformanceRepository,
    onPlace: (ActorPlacementReference) -> Unit,
    onCreatePerformance: ((String, String) -> Unit)? = null,
    onImportAudio: () -> Unit = {},
    onRecordAudio: () -> Unit = {},
    onImportVideo: () -> Unit = {},
    /** Opens the shared [com.donnclab.kinetiframe.library.ui.LibraryPickerDialog]. EditorScreen
     *  routes the picked item to a track by its kind, as it does for the media imports above.
     *  One browse surface covers performances, audio and backgrounds, so there is no separate
     *  library entry per asset type. */
    onFromLibrary: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showActorPicker by remember { mutableStateOf(false) }
    var selectedActor by remember { mutableStateOf<ActorManifest?>(null) }
    var actorItems by remember { mutableStateOf<List<ActorManifest>>(emptyList()) }
    var performanceItems by remember { mutableStateOf<List<PerformanceDocument>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun closeMenu() {
        expanded = false
        showActorPicker = false
        selectedActor = null
    }

    LaunchedEffect(showActorPicker, selectedActor?.actorId) {
        if (!showActorPicker) return@LaunchedEffect
        when (val result = withContext(Dispatchers.IO) { actors.list() }) {
            is StoreResult.Value -> actorItems = result.value
            is StoreResult.Failure -> error = result.message
        }
        performanceItems = emptyList()
        selectedActor?.let { actor ->
            when (val result = withContext(Dispatchers.IO) { performances.listForActor(actor.actorId) }) {
                is StoreResult.Value -> performanceItems = result.value
                is StoreResult.Failure -> error = result.message
            }
        }
    }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, "Add assets")
        }
        TimelineAssetSourceMenu(
            expanded && !showActorPicker,
            ::closeMenu,
            onActor = { showActorPicker = true },
            onImportAudio = {
                onImportAudio()
                closeMenu()
            },
            onRecordAudio = {
                onRecordAudio()
                closeMenu()
            },
            onVideo = {
                onImportVideo()
                closeMenu()
            },
            onLibrary = {
                onFromLibrary()
                closeMenu()
            },
        )
        DropdownMenu(expanded && showActorPicker, { closeMenu() }) {
            if (selectedActor == null) {
                DropdownMenuItem(text = { Text("‹ Add") }, onClick = { showActorPicker = false })
                if (actorItems.isEmpty()) {
                    DropdownMenuItem(text = {
                        Text("No saved actors")
                    }, enabled = false, onClick = {})
                }
                actorItems.forEach { actor ->
                    DropdownMenuItem(text = { Text(actor.displayName) }, onClick = {
                        selectedActor =
                            actor
                    })
                }
            } else {
                val actor = selectedActor ?: return@DropdownMenu
                DropdownMenuItem(text = { Text("‹ ${actor.displayName}") }, onClick = { selectedActor = null })
                PerformanceChooserItems(
                    performances = performanceItems,
                    onCreatePerformance = onCreatePerformance?.let { create ->
                        {
                            create(actor.actorId, projectId)
                            closeMenu()
                        }
                    },
                    onChoose = { performance ->
                        closeMenu()
                        scope.launch {
                            when (
                                val copied =
                                    withContext(Dispatchers.IO) { performances.copyIntoProject(performance, projectId) }
                            ) {
                                is StoreResult.Failure -> error = copied.message

                                is StoreResult.Value -> {
                                    val local = copied.value
                                    onPlace(
                                        ActorPlacementReference(
                                            "placement-${UUID.randomUUID()}",
                                            local.performanceId,
                                            local.actorId,
                                            local.actorRevisionId,
                                            "${actor.displayName} · ${local.displayName}",
                                            currentFrame,
                                            durationFramesFor(local, fps),
                                            zIndex = currentFrame,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Actor performances") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}

@Preview
@Composable
private fun ActorPerformancePickerPreview() { }
