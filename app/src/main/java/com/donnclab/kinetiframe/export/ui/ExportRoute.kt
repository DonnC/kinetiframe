package com.donnclab.kinetiframe.export.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.export.data.ExportPresets
import com.donnclab.kinetiframe.export.data.ProjectExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface ExportPhase {
    data object ChoosingPreset : ExportPhase
    data object Rendering : ExportPhase
    data class Done(val file: File, val displayName: String) : ExportPhase
    data class Failed(val message: String) : ExportPhase
}

/**
 * The finished-project export screen: a platform-target picker, then render progress, then the
 * deliberately simple two-choice delivery [ProjectExporter] itself documents -- Save to Gallery or
 * Share. The picker only ever chooses a destination resolution to letterbox into ([ExportPreset]);
 * it never touches the project's own canvas size or FPS, which stay exactly what they were chosen
 * as at project creation.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExportRoute(
    projects: ProjectRepository,
    newExporter: () -> ProjectExporter,
    projectId: String,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val exporter = remember { newExporter() }
    var progress by remember { mutableStateOf(0f) }
    var phase by remember { mutableStateOf<ExportPhase>(ExportPhase.ChoosingPreset) }
    var displayName by remember { mutableStateOf(projectId) }
    var selectedPreset by remember { mutableStateOf(ExportPresets.Presets.first()) }

    LaunchedEffect(projectId) {
        val manifest = when (
            val loaded = withContext(Dispatchers.IO) {
                projects.load(projectId)
            }
        ) {
            is StoreResult.Value -> loaded.value.first
            is StoreResult.Failure -> null
        }
        displayName = manifest?.displayName ?: projectId
    }

    fun startExport() {
        phase = ExportPhase.Rendering
        scope.launch {
            when (
                val result = exporter.export(projectId, selectedPreset.dimensions) { fraction ->
                    progress = fraction
                }
            ) {
                is StoreResult.Value -> phase = ExportPhase.Done(result.value, displayName)
                is StoreResult.Failure -> phase = ExportPhase.Failed(result.message)
            }
        }
    }

    // Only needed below Android 10 -- Scoped Storage on API 29+ lets the MediaStore insert in
    // saveToGallery succeed with no runtime permission at all (see the manifest entry's own
    // comment for why the underlying permission is capped with maxSdkVersion).
    val legacyStoragePermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val current = phase
                if (current is ExportPhase.Done) scope.launch { saveToGallery(exporter, current, snackbar) }
            } else {
                scope.launch {
                    snackbar.showSnackbar(
                        "Storage permission is needed to save to your gallery on this Android version",
                    )
                }
            }
        }

    fun requestSaveToGallery(done: ExportPhase.Done) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch { saveToGallery(exporter, done, snackbar) }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Export Video") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val current = phase) {
                is ExportPhase.ChoosingPreset -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Export as", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Only changes the exported video's resolution -- your project's own canvas is untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ExportPresets.Presets.forEach { preset ->
                        FilterChip(
                            selected = preset == selectedPreset,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset.label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(onClick = ::startExport, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                }

                is ExportPhase.Rendering -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Rendering your animation...", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is ExportPhase.Done -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Your animation is ready!", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { requestSaveToGallery(current) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save to Gallery")
                    }
                    OutlinedButton(onClick = {
                        exporter.shareVideo(current.file, current.displayName)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Share")
                    }
                }

                is ExportPhase.Failed -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Export failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(current.message, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onExit) { Text("Close") }
                }
            }
        }
    }
    BackHandler(onBack = onExit)
}

private suspend fun saveToGallery(exporter: ProjectExporter, done: ExportPhase.Done, snackbar: SnackbarHostState) {
    when (val result = withContext(Dispatchers.IO) { exporter.saveToGallery(done.file, done.displayName) }) {
        is StoreResult.Value -> snackbar.showSnackbar("Saved to your gallery")
        is StoreResult.Failure -> snackbar.showSnackbar(result.message)
    }
}
