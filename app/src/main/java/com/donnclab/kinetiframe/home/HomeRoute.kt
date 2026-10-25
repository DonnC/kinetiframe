package com.donnclab.kinetiframe.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.ui.LibraryPickerDialog
import com.donnclab.kinetiframe.library.ui.packs.PacksTabEffect
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabViewModel
import com.donnclab.kinetiframe.presentation.actors.ActorManagerEffect
import com.donnclab.kinetiframe.presentation.actors.ActorManagerIntent
import com.donnclab.kinetiframe.presentation.actors.ActorManagerViewModel
import com.donnclab.kinetiframe.project.ui.ProjectManagerEffect
import com.donnclab.kinetiframe.project.ui.ProjectManagerIntent
import com.donnclab.kinetiframe.project.ui.ProjectManagerViewModel
import com.donnclab.kinetiframe.ui.appViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * The app's Home destination: one tabbed dashboard (Projects / Packs / Actors, see [HomeScreen]).
 * This route owns the project and pack import, export and share plumbing.
 */
@Composable
fun HomeRoute(
    /** Lists library projects in the import picker, which gets its own ViewModel with the library screens. */
    libraryRepository: LibraryRepository,
    refreshToken: Int = 0,
    onOpenProject: (String) -> Unit,
    onOpenPackCreator: (String?) -> Unit = {},
    onOpenExport: (String) -> Unit = {},
    onOpenActor: (String) -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val projectModel: ProjectManagerViewModel = appViewModel()
    val actorModel: ActorManagerViewModel = appViewModel()
    val hierarchyModel: com.donnclab.kinetiframe.library.ui.HierarchyBrowserViewModel = appViewModel()
    val hierarchyState by hierarchyModel.state.collectAsStateWithLifecycle()
    val packModel: PacksTabViewModel = appViewModel()

    val projectState by projectModel.state.collectAsStateWithLifecycle()
    val actorState by actorModel.state.collectAsStateWithLifecycle()
    val packState by packModel.state.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLibraryImportPicker by remember { mutableStateOf(false) }
    val actions: HomeActionsViewModel = appViewModel()
    val actionsState by actions.state.collectAsStateWithLifecycle()

    if (actionsState.cleanupOffered) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { actions.handleIntent(HomeActionsIntent.DismissCleanup) },
            title = { androidx.compose.material3.Text("Clean the trial library?") },
            text = {
                androidx.compose.material3.Text(
                    "Keep Tavi presents, Tavi/Mira/Kuda, the seven curated packs and scene backgrounds. All other existing projects, actors and library items move to a recoverable archive. New work created after cleanup is kept.",
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    actions.handleIntent(HomeActionsIntent.DismissCleanup)
                }) { androidx.compose.material3.Text("Not now") }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    actions.handleIntent(HomeActionsIntent.CleanUp)
                }) { androidx.compose.material3.Text("Clean up") }
            },
        )
    }

    // Home stays in the back stack while an editor is open, so its ViewModels only ever ran their
    // own `init { reload() }` once -- anything saved in a child screen (a new actor revision, a
    // saved pack, a project) was invisible here until the process restarted. MainActivity bumps
    // [refreshToken] on every pop; re-reading all three lists on that change is cheap next to
    // silently showing the animator stale work. Skipped at 0 so the first composition doesn't
    // duplicate the load each ViewModel already performs on construction.
    LaunchedEffect(refreshToken) {
        if (refreshToken > 0) {
            projectModel.handleIntent(ProjectManagerIntent.Reload)
            actorModel.handleIntent(ActorManagerIntent.Reload)
            packModel.handleIntent(PacksTabIntent.Reload)
        }
    }

    LaunchedEffect(projectModel) {
        projectModel.effect.collect { effect ->
            when (effect) {
                is ProjectManagerEffect.OpenProject -> onOpenProject(effect.projectId)
                is ProjectManagerEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }
    LaunchedEffect(actorModel) {
        actorModel.effect.collect { effect ->
            when (effect) {
                is ActorManagerEffect.OpenActorStudio -> onOpenActor(effect.actorId)
                is ActorManagerEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }
    LaunchedEffect(packModel) {
        packModel.effect.collect { effect ->
            when (effect) {
                is PacksTabEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(actions) {
        actions.effect.collect { effect ->
            when (effect) {
                is HomeActionsEffect.Share -> shareFile(context, effect.file, effect.title)

                is HomeActionsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)

                HomeActionsEffect.ProjectsChanged -> projectModel.handleIntent(ProjectManagerIntent.Reload)

                HomeActionsEffect.EverythingChanged -> {
                    projectModel.handleIntent(ProjectManagerIntent.Reload)
                    actorModel.handleIntent(ActorManagerIntent.Reload)
                    packModel.handleIntent(PacksTabIntent.Reload)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        actions.handleIntent(HomeActionsIntent.ImportArchive { context.contentResolver.openInputStream(uri) })
    }

    // On-disk pack drawings for each Packs-tab card's 2x2 collage, read in place, never copied.
    val packPreviewFiles = remember(packState.previewItems) {
        packState.previewItems.mapValues { (itemId, entries) ->
            entries.map { packModel.packItemFile(itemId, it.file) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.padding(padding)) {
            HomeScreen(
                hierarchyState = hierarchyState,
                onHierarchyIntent = hierarchyModel::handleIntent,
                projectState = projectState,
                onProjectIntent = projectModel::handleIntent,
                packState = packState,
                packPreviewFiles = packPreviewFiles,
                onPackIntent = packModel::handleIntent,
                actorState = actorState,
                onActorIntent = actorModel::handleIntent,
                onShareProject = { actions.handleIntent(HomeActionsIntent.ShareProject(it)) },
                onSaveToLibraryProject = { actions.handleIntent(HomeActionsIntent.SaveToLibrary(it)) },
                onExportVideoProject = { project -> onOpenExport(project.projectId) },
                onSharePack = { actions.handleIntent(HomeActionsIntent.SharePack(it)) },
                onImportFromFile = { importLauncher.launch("*/*") },
                onImportFromLibrary = { showLibraryImportPicker = true },
                onOpenPackCreator = onOpenPackCreator,
                detailsPreviewFile = { itemId, entry -> packModel.packItemFile(itemId, entry.file) },
                // The bottom-nav Library tab opens the full browse screen. The Import sheet's
                // "From Library" row opens LibraryPickerDialog to pick a single project instead.
                onBrowseLibrary = onOpenLibrary,
                onSettingsTapped = onOpenSettings,
            )
        }
    }

    if (showLibraryImportPicker) {
        LibraryPickerDialog(
            library = libraryRepository,
            allowedKinds = setOf(LibraryItemKind.PROJECT),
            onDismiss = { showLibraryImportPicker = false },
            onPick = { item ->
                showLibraryImportPicker = false
                actions.handleIntent(HomeActionsIntent.ImportFromLibrary(item))
            },
        )
    }
}

/** Offers [file] to other apps through the share sheet, through the app's file provider. */
private fun shareFile(context: android.content.Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}
