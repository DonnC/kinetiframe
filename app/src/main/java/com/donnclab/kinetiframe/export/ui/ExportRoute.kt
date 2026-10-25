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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.export.model.ExportPresets
import com.donnclab.kinetiframe.presentation.common.shareFile
import com.donnclab.kinetiframe.ui.appViewModel
import kotlinx.coroutines.launch

/**
 * The finished-project export screen: a platform-target picker, then render progress, then the
 * deliberately simple two-choice delivery the exporter itself documents -- Save to Gallery or
 * Share. The picker only ever chooses a destination resolution to letterbox into
 * (`ExportPreset`); it never touches the project's own canvas size or FPS, which stay exactly
 * what they were chosen as at project creation.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExportRoute(projectId: String, onExit: () -> Unit) {
    val model: ExportViewModel = appViewModel(key = "export-$projectId") {
        set(ExportViewModel.ProjectIdKey, projectId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                is ExportEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
                is ExportEffect.Share -> shareFile(context, effect.file, effect.title, "video/mp4")
            }
        }
    }

    // Only needed below Android 10 -- Scoped Storage on API 29+ lets the MediaStore insert in
    // saveToGallery succeed with no runtime permission at all (see the manifest entry's own
    // comment for why the underlying permission is capped with maxSdkVersion).
    val legacyStoragePermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                model.handleIntent(ExportIntent.SaveToGallery)
            } else {
                scope.launch {
                    snackbar.showSnackbar(
                        "Storage permission is needed to save to your gallery on this Android version",
                    )
                }
            }
        }

    fun requestSaveToGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            model.handleIntent(ExportIntent.SaveToGallery)
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Export Video") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val current = state.phase) {
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
                            selected = preset == state.preset,
                            onClick = { model.handleIntent(ExportIntent.ChoosePreset(preset)) },
                            label = { Text(preset.label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = { model.handleIntent(ExportIntent.Start) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Continue") }
                }

                is ExportPhase.Rendering -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Rendering your animation...", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${(state.progress * 100).toInt()}%",
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
                    Button(onClick = ::requestSaveToGallery, modifier = Modifier.fillMaxWidth()) {
                        Text("Save to Gallery")
                    }
                    OutlinedButton(
                        onClick = { model.handleIntent(ExportIntent.Share) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
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
