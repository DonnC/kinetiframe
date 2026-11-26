package com.donnclab.kinetiframe.presentation.export

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.export.model.ExportPreset
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AddFlowTopBar
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.presentation.common.readableWidth
import com.donnclab.kinetiframe.presentation.common.shareFile
import com.donnclab.kinetiframe.presentation.common.viewFile
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
    val storagePermissionMessage = stringResource(R.string.export_storage_permission_needed)
    val resources = LocalResources.current
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                is ExportEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(resources))
                is ExportEffect.Share -> shareFile(context, effect.file, effect.title.asString(resources), "video/mp4")
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
                scope.launch { snackbar.showSnackbar(storagePermissionMessage) }
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
        // A way back without finishing an export (R3-7); the system Back does the same.
        topBar = { AddFlowTopBar(title = stringResource(R.string.export_title), onBack = onExit) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExportContent(
            state,
            ExportActions(
                onChoose = { model.handleIntent(ExportIntent.ChoosePreset(it)) },
                onStart = { model.handleIntent(ExportIntent.Start) },
                onWatch = {
                    (state.phase as? ExportPhase.Done)?.let { done ->
                        // Watch the video before saving or sharing it (R3-7).
                        if (!viewFile(context, done.file, "video/mp4")) {
                            scope.launch { snackbar.showSnackbar(resources.getString(R.string.export_no_player)) }
                        }
                    }
                },
                onSave = ::requestSaveToGallery,
                onShare = { model.handleIntent(ExportIntent.Share) },
                onClose = onExit,
            ),
            Modifier.padding(padding),
        )
    }
    BackHandler(onBack = onExit)
}
