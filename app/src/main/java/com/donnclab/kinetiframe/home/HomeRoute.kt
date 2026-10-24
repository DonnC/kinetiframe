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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.actors.data.ActorReferenceIndex
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.ui.ActorManagerEffect
import com.donnclab.kinetiframe.actors.ui.ActorManagerIntent
import com.donnclab.kinetiframe.actors.ui.ActorManagerViewModel
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.data.db.AppDatabase
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackArchiver
import com.donnclab.kinetiframe.library.ui.LibraryPickerDialog
import com.donnclab.kinetiframe.library.ui.packs.PacksTabEffect
import com.donnclab.kinetiframe.library.ui.packs.PacksTabIntent
import com.donnclab.kinetiframe.library.ui.packs.PacksTabViewModel
import com.donnclab.kinetiframe.project.data.KframeArchiver
import com.donnclab.kinetiframe.project.ui.ProjectManagerEffect
import com.donnclab.kinetiframe.project.ui.ProjectManagerIntent
import com.donnclab.kinetiframe.project.ui.ProjectManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The app's Home destination: one tabbed dashboard (Projects / Packs / Actors, see [HomeScreen]).
 * This route owns the project and pack import, export and share plumbing.
 */
@Composable
fun HomeRoute(
    storageRoot: File,
    refreshToken: Int = 0,
    onOpenProject: (String) -> Unit,
    onOpenPackCreator: (String?) -> Unit = {},
    onOpenExport: (String) -> Unit = {},
    onOpenActor: (String) -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val files = remember(storageRoot) { DocumentFileStore(storageRoot) }
    val db = remember(context) { AppDatabase.getInstance(context) }

    val projectModel: ProjectManagerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProjectManagerViewModel(ProjectRepository(files), db.projectDao()) as T
        },
    )
    val actorModel: ActorManagerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val actors = ActorRepository(files, dao = db.actorDao())
                val performances = PerformanceRepository(files, dao = db.performanceDao())
                return ActorManagerViewModel(
                    actors,
                    ActorReferenceIndex(files),
                    ActorBundleRepository(files, actors, performances, libraryDao = db.libraryItemDao()),
                ) as T
            }
        },
    )
    val libraryRepository = remember(files, context) { LibraryRepository(files, dao = db.libraryItemDao()) }
    val hierarchyModel: com.donnclab.kinetiframe.library.ui.HierarchyBrowserViewModel =
        viewModel(
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    com.donnclab.kinetiframe.library.ui.HierarchyBrowserViewModel(
                        libraryRepository.organizationStore(),
                    ) as T
            },
        )
    val hierarchyState by hierarchyModel.state.collectAsStateWithLifecycle()
    val packModel: PacksTabViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PacksTabViewModel(libraryRepository) as T
        },
    )

    val projectState by projectModel.state.collectAsStateWithLifecycle()
    val actorState by actorModel.state.collectAsStateWithLifecycle()
    val packState by packModel.state.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLibraryImportPicker by remember { mutableStateOf(false) }
    var showCleanupConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(files) {
        val demos = withContext(Dispatchers.IO) {
            com.donnclab.kinetiframe.library.bundled.BundledDemoInstaller(
                files,
                context.assets::open,
                libraryRepository,
            ).install()
        }
        val resources = withContext(Dispatchers.IO) {
            com.donnclab.kinetiframe.library.bundled.BundledDemoLibraryInstaller(
                files,
                context.assets::open,
                libraryRepository,
            ).install()
        }
        val actorSamples = withContext(Dispatchers.IO) {
            com.donnclab.kinetiframe.library.bundled.BundledActorSampleInstaller(files, context.assets::open).install()
        }
        showCleanupConfirmation = withContext(Dispatchers.IO) {
            com.donnclab.kinetiframe.library.bundled.CuratedLibraryMigration(files).pending()
        }
        if (demos.installed > 0 || actorSamples.installed > 0) {
            projectModel.handleIntent(ProjectManagerIntent.Reload)
            actorModel.handleIntent(ActorManagerIntent.Reload)
            packModel.handleIntent(PacksTabIntent.Reload)
        }
        if (demos.errors.isNotEmpty() || resources.errors.isNotEmpty() ||
            actorSamples.errors.isNotEmpty()
        ) {
            snackbar.showSnackbar("Some demo resources could not be installed. Your work was not replaced.")
        }
    }

    if (showCleanupConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCleanupConfirmation = false },
            title = { androidx.compose.material3.Text("Clean the trial library?") },
            text = {
                androidx.compose.material3.Text(
                    "Keep Tavi presents, Tavi/Mira/Kuda, the seven curated packs and scene backgrounds. All other existing projects, actors and library items move to a recoverable archive. New work created after cleanup is kept.",
                )
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCleanupConfirmation = false
                }) { androidx.compose.material3.Text("Not now") }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCleanupConfirmation = false
                    scope.launch {
                        when (
                            val result =
                                withContext(Dispatchers.IO) {
                                    com.donnclab.kinetiframe.library.bundled.CuratedLibraryMigration(files).migrate()
                                }
                        ) {
                            is StoreResult.Value -> {
                                projectModel.handleIntent(ProjectManagerIntent.Reload)
                                actorModel.handleIntent(ActorManagerIntent.Reload)
                                packModel.handleIntent(PacksTabIntent.Reload)
                                snackbar.showSnackbar("Trial library archived. Your curated library is ready.")
                            }

                            is StoreResult.Failure -> snackbar.showSnackbar(result.message)
                        }
                    }
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

    // -- Project export/share/import --

    fun shareProject(project: ProjectManifest) {
        scope.launch {
            val destination = File(File(context.filesDir, "kframe-exports"), "${project.projectId}.kframe")
            val result = withContext(Dispatchers.IO) { KframeArchiver.export(files, project.projectId, destination) }
            when (result) {
                is StoreResult.Value -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.value)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share ${project.displayName}"))
                }

                is StoreResult.Failure -> snackbar.showSnackbar(result.message)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val tempFile = File(context.cacheDir, "import-${System.currentTimeMillis()}.kframe")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Could not read the selected file")
                    KframeArchiver.import(files, tempFile)
                }.getOrElse { StoreResult.Failure(it.message ?: "Could not import this project") }
                    .also { tempFile.delete() }
            }
            when (result) {
                is StoreResult.Value -> {
                    projectModel.handleIntent(ProjectManagerIntent.Reload)
                    snackbar.showSnackbar("Project imported")
                }

                is StoreResult.Failure -> snackbar.showSnackbar(result.message)
            }
        }
    }

    fun saveProjectToLibrary(project: ProjectManifest) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val tempFile = File(context.cacheDir, "library-project-${System.currentTimeMillis()}.kframe")
                    when (val exported = KframeArchiver.export(files, project.projectId, tempFile)) {
                        is StoreResult.Value -> Unit
                        is StoreResult.Failure -> throw IllegalStateException(exported.message, exported.cause)
                    }
                    libraryRepository.exportAsset(
                        kind = LibraryItemKind.PROJECT,
                        itemId = "library-${UUID.randomUUID()}",
                        displayName = project.displayName,
                        sourceFile = tempFile,
                        projectId = project.projectId,
                        nowEpochMs = System.currentTimeMillis(),
                    ).also { tempFile.delete() }
                }.getOrElse { StoreResult.Failure(it.message ?: "Could not save this project to your library") }
            }
            when (result) {
                is StoreResult.Value -> snackbar.showSnackbar("Saved ${project.displayName} to your library")
                is StoreResult.Failure -> snackbar.showSnackbar(result.message)
            }
        }
    }

    fun importProjectFromPick(manifest: LibraryItemManifest) {
        showLibraryImportPicker = false
        scope.launch {
            val tempFile = File(context.cacheDir, "library-import-${System.currentTimeMillis()}.kframe")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (val copied = libraryRepository.copyAssetTo(manifest.libraryItemId, tempFile)) {
                        is StoreResult.Value -> Unit
                        is StoreResult.Failure -> throw IllegalStateException(copied.message, copied.cause)
                    }
                    KframeArchiver.import(files, tempFile)
                }.getOrElse { StoreResult.Failure(it.message ?: "Could not import this project") }
                    .also { tempFile.delete() }
            }
            when (result) {
                is StoreResult.Value -> {
                    projectModel.handleIntent(ProjectManagerIntent.Reload)
                    snackbar.showSnackbar("Project imported")
                }

                is StoreResult.Failure -> snackbar.showSnackbar(result.message)
            }
        }
    }

    // -- Pack share --
    fun sharePack(pack: LibraryItemManifest) {
        scope.launch {
            val destination = File(File(context.filesDir, "pack-exports"), "${pack.displayName}.kpack")
            val result = withContext(Dispatchers.IO) {
                PackArchiver.export(files, libraryRepository, pack.libraryItemId, destination, LibraryScope.PERSONAL)
            }
            when (result) {
                is StoreResult.Value -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.value)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share ${pack.displayName}"))
                }

                is StoreResult.Failure -> snackbar.showSnackbar(result.message)
            }
        }
    }

    // On-disk pack drawings for each Packs-tab card's 2x2 collage, read in place, never copied.
    val packPreviewFiles = remember(packState.previewItems) {
        packState.previewItems.mapValues { (itemId, entries) ->
            entries.map { libraryRepository.packItemFile(itemId, it.file) }
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
                onShareProject = ::shareProject,
                onSaveToLibraryProject = ::saveProjectToLibrary,
                onExportVideoProject = { project -> onOpenExport(project.projectId) },
                onSharePack = ::sharePack,
                onImportFromFile = { importLauncher.launch("*/*") },
                onImportFromLibrary = { showLibraryImportPicker = true },
                onOpenPackCreator = onOpenPackCreator,
                detailsPreviewFile = { itemId, entry -> libraryRepository.packItemFile(itemId, entry.file) },
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
            onPick = ::importProjectFromPick,
        )
    }
}
