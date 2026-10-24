package com.donnclab.kinetiframe.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.navigation.BottomNavDestination
import kotlinx.coroutines.launch

/** Wires [LibraryViewModel] to [LibraryScreen]. */
@Composable
fun LibraryRoute(
    onNavigate: (BottomNavDestination) -> Unit,
    onOpenDocument: (LibraryOpenTarget) -> Unit = {},
    onEditPack: (com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val files =
        remember(context) {
            com.donnclab.kinetiframe.data.storage.DocumentFileStore(java.io.File(context.filesDir, "actor-studio"))
        }
    val repository = remember(files) {
        com.donnclab.kinetiframe.library.data.LibraryRepository(
            files,
            dao = com.donnclab.kinetiframe.project.data.db.AppDatabase.getInstance(context).libraryItemDao(),
        )
    }
    val viewModel: LibraryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(LocalLibraryCatalog(files, repository)) as T
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(viewModel) {
        viewModel.handleIntent(LibraryIntent.Reload)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel, files) {
        val seeded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.donnclab.kinetiframe.library.bundled.BundledDemoLibraryInstaller(
                files,
                context.assets::open,
                repository,
            ).install()
        }
        viewModel.handleIntent(LibraryIntent.Reload)
        if (seeded.errors.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                "Some starter assets could not be added to Library; they will retry next launch.",
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LibraryEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is LibraryEffect.OpenDocument -> onOpenDocument(effect.target)
                is LibraryEffect.EditPack -> onEditPack(effect.item)
            }
        }
    }

    // The activity owns destinations, including persisted Settings.
    val handleNavigate: (BottomNavDestination) -> Unit = { destination ->
        onNavigate(destination)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.padding(padding)) {
            LibraryScreen(state = state, onIntent = viewModel::handleIntent, onNavigate = handleNavigate)
        }
    }
}
