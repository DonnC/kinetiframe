package com.donnclab.kinetiframe.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.donnclab.kinetiframe.ui.components.common.CompactTextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.components.EditorTopBar
import com.donnclab.kinetiframe.ui.components.GridOptionsPopup
import com.donnclab.kinetiframe.ui.components.LayerPanelPopup
import com.donnclab.kinetiframe.ui.components.OnionOptionsPopup
import com.donnclab.kinetiframe.ui.components.canvas.CanvasArea
import com.donnclab.kinetiframe.ui.components.audio.AudioRecorderActions
import com.donnclab.kinetiframe.ui.components.audio.AudioRecorderSheet
import com.donnclab.kinetiframe.ui.components.dialogs.TextInsertionDialog
import com.donnclab.kinetiframe.ui.components.dock.SelectionDock
import com.donnclab.kinetiframe.ui.components.dock.ToolDock
import com.donnclab.kinetiframe.ui.components.studio.FrameStudio
import com.donnclab.kinetiframe.ui.components.timeline.LayerPanel
import com.donnclab.kinetiframe.ui.components.timeline.TimelineControls
import com.donnclab.kinetiframe.ui.components.timeline.TimelineTracksPanel
import com.donnclab.kinetiframe.ui.components.timeline.ActorPerformancePicker
import com.donnclab.kinetiframe.ui.components.rigging.PuppetTopBar
import com.donnclab.kinetiframe.ui.components.rigging.PartActionsRow
import com.donnclab.kinetiframe.ui.components.rigging.PartPickerSheet
import com.donnclab.kinetiframe.ui.components.rigging.PartRail
import com.donnclab.kinetiframe.ui.components.rigging.PresetLibrarySheet
import com.donnclab.kinetiframe.ui.components.timeline.durationFramesFor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    actorReference: com.donnclab.kinetiframe.ui.actors.ActorReferenceSession? = null,
    initialEditorState: StudioState? = null,
    /** Non-null only when opened from the project gallery (see MainActivity's EditorKey route);
     *  null for the embedded Actor Studio canvas and for Pack Creator's own scratch drafts. */
    projectId: String? = null,
    /** A stable identity for a non-project session's own scratch cache -- e.g. Actor Studio
     *  passes "actor-$actorId-$revisionId" so returning to the SAME actor/revision keeps seeing
     *  its own frames, while two DIFFERENT actors never share a directory. Only meaningful when
     *  [projectId] is null; ignored otherwise. Without a namespace, every session would share one
     *  directory, and because un-namespaced frames share the fixed "main" layer cache key, a new
     *  actor or pack draft would load the previous session's frame bitmaps. */
    cacheNamespace: String? = null,
    onSaveActorRevision: ((com.donnclab.kinetiframe.engine.model.PuppetDefinition, com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager) -> Unit)? = null,
    onSavePerformance: ((com.donnclab.kinetiframe.engine.model.PuppetInstance, Long) -> Unit)? = null,
    onPerformanceActions: (() -> Unit)? = null,
    actorViews: (@Composable () -> Unit)? = null,
    onExitActorStudio: (() -> Unit)? = null,
    onCreatePerformance: ((String, String) -> Unit)? = null,
    onEditPerformance: ((com.donnclab.kinetiframe.engine.model.ActorPlacementReference, String) -> Unit)? = null,
    runtimeRigAssetManager: com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager? = null,
    returnedPerformance: com.donnclab.kinetiframe.data.actor.PerformanceDocument? = null,
    onReturnedPerformanceConsumed: () -> Unit = {},
    /** Actor Studio observes only durable rig/gesture edits, never transient selection or preview state. */
    onActorStudioDirtyChanged: ((Boolean) -> Unit)? = null,
    onActorSaveActionAvailable: (((() -> Unit)?) -> Unit)? = null,
    onActorSavePreparationFailed: (() -> Unit)? = null,
    /** Non-null only when this screen is hosting the Pack Creator micro-editor
     *  ([com.donnclab.kinetiframe.ui.packs.PackCreatorRoute]) rather than an ordinary project. */
    packCreatorConfig: com.donnclab.kinetiframe.ui.packs.PackCreatorConfig? = null,
    onSettingsClick: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val playbackHidesTools = com.donnclab.kinetiframe.ui.settings.LocalAppPreferences.current.hideToolsDuringPlayback
    var returnHomeAfterSave by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitDestination by remember { mutableStateOf<(() -> Unit)?>(null) }
    if (returnHomeAfterSave) AlertDialog(
        onDismissRequest = {}, text = { Text("Saving project…") }, confirmButton = {})
    val actorStore = remember { com.donnclab.kinetiframe.data.actor.ActorFileStore(File(context.filesDir, "actor-studio")) }
    val appDatabase = remember(context) { com.donnclab.kinetiframe.data.project.db.AppDatabase.getInstance(context) }
    val actorRepository = remember(actorStore, appDatabase) { com.donnclab.kinetiframe.data.actor.ActorRepository(actorStore, dao = appDatabase.actorDao()) }
    val performanceRepository = remember(actorStore, appDatabase) { com.donnclab.kinetiframe.data.actor.PerformanceRepository(actorStore, dao = appDatabase.performanceDao()) }
    val actorBundles = remember(actorStore, actorRepository, performanceRepository, appDatabase) {
        com.donnclab.kinetiframe.data.actor.ActorBundleRepository(actorStore, actorRepository, performanceRepository, libraryDao = appDatabase.libraryItemDao())
    }
    val libraryRepository = remember(actorStore, appDatabase) { com.donnclab.kinetiframe.data.library.LibraryRepository(actorStore, dao = appDatabase.libraryItemDao()) }
    val projectRepository = remember(actorStore) { com.donnclab.kinetiframe.data.project.ProjectRepository(actorStore) }

    // Saved frames/assets remain project-owned. Raster edits use a private working cache;
    // evictions are not implicit saves, so Discard preserves the committed project pixels.
    // Pack Creator already owns a disposable project and keeps its existing save adapter.
    // A caller with no project ID -- Actor Studio's embedded canvas, or any other scratch session
    // -- gets its own namespaced subdirectory instead of ever touching a directory another
    // session could also land in: [cacheNamespace] when the caller supplies one (Actor Studio's
    // actor+revision identity), or a random one generated once per composition as a last resort
    // so an un-namespaced caller still can't collide with anyone else.
    val fallbackCacheNamespace = remember { "session-${java.util.UUID.randomUUID()}" }
    val projectDraft = if (projectId != null && packCreatorConfig == null)
        viewModel<com.donnclab.kinetiframe.ui.projects.ProjectDraftSession>(key = "project-draft-$projectId") else null
    val editorCacheDir = remember(projectId, cacheNamespace) {
        projectId?.let {
            if (packCreatorConfig != null) File(actorStore.projectDirectory(it), "frames")
            else File(context.cacheDir, "editor-scratch/${requireNotNull(projectDraft).state.value.id}/frames")
        }
            ?: File(context.cacheDir, "editor-scratch/${cacheNamespace ?: fallbackCacheNamespace}")
    }
    val editorFilesDir = remember(projectId, cacheNamespace) {
        projectId?.let { actorStore.projectDirectory(it) }
            ?: File(context.filesDir, "editor-scratch/${cacheNamespace ?: fallbackCacheNamespace}").apply { mkdirs() }
    }

    var projectLoading by remember(projectId) { mutableStateOf(projectId != null && initialEditorState == null) }
    var projectLoadError by remember(projectId) { mutableStateOf<String?>(null) }
    var loadedProjectDocument by remember(projectId) { mutableStateOf<com.donnclab.kinetiframe.data.project.ProjectSaveDocument?>(null) }

    LaunchedEffect(projectId) {
        if (projectId == null || initialEditorState != null) return@LaunchedEffect
        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val loaded = projectRepository.load(projectId)
            if (loaded is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value && projectDraft?.state?.value?.ready == false) {
                try {
                    com.donnclab.kinetiframe.data.project.ProjectFrameDraft.open(File(actorStore.projectDirectory(projectId), "frames"), editorCacheDir)
                } catch (failure: Exception) {
                    return@withContext com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure(failure.message ?: "Could not open frame draft", failure)
                }
            }
            loaded
        }
        when (result) {
            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> {
                loadedProjectDocument = result.value.second
                projectDraft?.handleIntent(com.donnclab.kinetiframe.ui.projects.ProjectDraftSessionIntent.Prepared)
            }
            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> projectLoadError = result.message
        }
        projectLoading = false
    }

    if (projectLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }
    if (projectLoadError != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text("Could not open this project: ${projectLoadError}")
        }
        return
    }

    // Only the structural, content-bearing part of a saved project round-trips here -- transient
    // view state (zoom/pan, tool selection, onion-skin/grid settings) resets to defaults on load.
    val effectiveInitialState = initialEditorState ?: loadedProjectDocument?.let { doc ->
        StudioState(
            project = doc.project,
            scene = SceneState(nodes = doc.project.nodes),
            animation = AnimationState(totalFrames = doc.totalFrames, fps = doc.project.fps),
            layers = LayerState(layers = doc.frameLayers[0] ?: doc.project.layers, frameLayers = doc.frameLayers,
                linkedCels = doc.linkedCels,
                activeLayerIndex = doc.activeLayerIndex.coerceIn(0, ((doc.frameLayers[0] ?: doc.project.layers).size - 1).coerceAtLeast(0)))
        )
    }

    // WHY an explicit key: `viewModel()` without one resolves against the default key for the
    // ViewModel *class*, so every EditorScreen in the app -- a real project, a pack draft, Actor
    // Studio's embedded sketch canvas -- competed for a single StudioViewModel slot and whoever
    // asked second silently inherited the first one's canvas (the factory is not called on a cache
    // hit, so `effectiveInitialState`, [editorCacheDir] and [editorFilesDir] were all thrown away).
    // MainActivity's rememberViewModelStoreNavEntryDecorator now scopes stores per NavEntry, which
    // fixes the cross-destination case; this key closes the remaining in-entry case -- one entry
    // hosting more than one document (Actor Studio hosts its own canvas, and a revision switch is
    // a different document) must never reuse the previous document's engine state. Same identity
    // that scopes the frame cache above, so the ViewModel and its pixels can never disagree.
    val documentKey = remember(projectId, cacheNamespace) {
        "editor-${projectId ?: cacheNamespace ?: fallbackCacheNamespace}"
    }
    val viewModel: StudioViewModel = viewModel(
        key = documentKey,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StudioViewModel(editorCacheDir,
                    loadActorPresets = com.donnclab.kinetiframe.data.ActorPresetRepository(context.assets)::load,
                    initialEditorState = effectiveInitialState?.copy(packTags = packCreatorConfig?.tags.orEmpty()),
                    runtimeRigAssetManager = runtimeRigAssetManager,
                    filesDir = editorFilesDir, diagnosticsDir = context.filesDir) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var cameraPreview by remember(state.project.id) {
        mutableStateOf(com.donnclab.kinetiframe.ui.components.camera.CameraWorkspacePolicy.PREVIEW_ON_OPEN)
    }
    val placementScope = rememberCoroutineScope()
    var placementError by remember { mutableStateOf<String?>(null) }
    val placementPreparation = remember(actorRepository, performanceRepository, actorStore) {
        com.donnclab.kinetiframe.domain.render.ActorPlacementPreparation(actorRepository, performanceRepository, actorStore)
    }
    fun placeActor(reference: com.donnclab.kinetiframe.engine.model.ActorPlacementReference) {
        val project = state.project
        placementScope.launch {
            runCatching { withContext(Dispatchers.IO) { placementPreparation.prepare(reference, project) } }
                .onSuccess { if (viewModel.state.value.project.id == project.id) viewModel.handleIntent(StudioIntent.ActorPlacement.Add(it)) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it
                    placementError = it.message ?: "Could not place performance" }
        }
    }
    placementError?.let { message -> AlertDialog(onDismissRequest = { placementError = null },
        title = { Text("Performance placement") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { placementError = null }) { Text("OK") } }) }
    // Repair only legacy identity placements entirely outside the stage; never reposition an
    // explicitly transformed character or change an already visible saved composition.
    LaunchedEffect(state.project.id) {
        val project = state.project
        project.actorPlacements.values.forEach { reference ->
            runCatching { withContext(Dispatchers.IO) { placementPreparation.prepare(reference, project, repairOnly = true) } }
                .onSuccess { repaired -> if (repaired.transform != reference.transform)
                    viewModel.handleIntent(StudioIntent.ActorPlacement.UpdateTransform(reference.placementId, repaired.transform)) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
        }
    }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
        viewModel.handleIntent(StudioIntent.ActorMotion.CancelTake)
    }
    DisposableEffect(viewModel) { onDispose { viewModel.handleIntent(StudioIntent.ActorMotion.CancelTake) } }
    val actorStudioDirty = com.donnclab.kinetiframe.ui.actors.hasActorStudioChanges(initialEditorState, state)
    LaunchedEffect(actorStudioDirty) { onActorStudioDirtyChanged?.invoke(actorStudioDirty) }
    LaunchedEffect(returnedPerformance?.performanceId) {
        val performance = returnedPerformance ?: return@LaunchedEffect
        // Never consume a return result against a newly opened/different project. The result is
        // project-scoped and must remain available until the originating editor is restored.
        if (performance.projectId != state.project.id) return@LaunchedEffect
        if (performance.performanceId !in state.project.actorPlacements.values.map { it.performanceId }) {
            val frames = kotlin.math.ceil(performance.durationMicros * state.project.fps / 1_000_000.0).toInt().coerceAtLeast(1)
            placeActor(com.donnclab.kinetiframe.engine.model.ActorPlacementReference(
                placementId = "placement-${java.util.UUID.randomUUID()}", performanceId = performance.performanceId,
                actorId = performance.actorId, actorRevisionId = performance.actorRevisionId,
                label = performance.displayName, startFrame = state.animation.currentFrame, durationFrames = frames
            ))
        }
        onReturnedPerformanceConsumed()
    }
    val isPuppetStudio = state.scene.studioMode != StudioMode.RASTER_DRAWING
    /** True when this canvas was opened by Actor Studio against a single actor document, rather
     *  than as an ordinary project/pack editor -- Actor Studio is the only caller that seeds
     *  [initialEditorState]. Named once here so actor-only canvas furniture reads intentionally
     *  instead of re-deriving the same condition inline. */
    val isActorStudioSession = initialEditorState != null
    val frameReferences = viewModel.frameReferences
    DisposableEffect(frameReferences) { onDispose { frameReferences.clear() } }
    val frameReference = if (!isActorStudioSession && packCreatorConfig == null && !isPuppetStudio &&
        !state.animation.isPlaying) frameReferences.frame(state.animation.currentFrame) else null
    LaunchedEffect(frameReference) {
        // Returning to a frame restores its guide, not an old transform gesture/menu.
        frameReference?.editing = false
        frameReference?.menuOpen = false
    }
    var showPartPicker by remember { mutableStateOf(false) }
    var showPresetLibrary by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var toolbarVisible by remember { mutableStateOf(true) }
    var tracksWorkspaceVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.selection.selectedContent?.videos) {
        if (!state.selection.selectedContent?.videos.isNullOrEmpty()) tracksWorkspaceVisible = false
    }
    var pendingPackSaveFrames by remember { mutableStateOf<Int?>(null) }
    var packItemDetails by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    // Keep frame navigation position when focus mode temporarily removes the dock.
    val timelineScrollState = rememberLazyListState()
    var layerSheetVisible by remember { mutableStateOf(false) }
    var frameStudioVisible by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }
    var showOnionMenu by remember { mutableStateOf(false) }
    var actorLibraryMessage by remember { mutableStateOf<String?>(null) }
    var backgroundImageError by remember { mutableStateOf<String?>(null) }
    var timelineError by remember { mutableStateOf<String?>(null) }
    var projectSaveMessage by remember { mutableStateOf<String?>(null) }
    var textAddDialogState by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var textEditDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Keep engine errors observed while the source workspace replaces the main canvas.
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect -> when (effect) {
            is StudioEffect.ShowError -> timelineError = effect.message
            is StudioEffect.ShowTextAddDialog -> textAddDialogState = effect.x to effect.y
            is StudioEffect.ShowTextEditDialog -> textEditDialogState = effect.id to effect.initialText
        } }
    }

    // Building and writing a project's document is cheap (small JSON); the frame pixels
    // themselves only reach disk for certain once persistAllToDisk below has run, since a plain
    // cache put only invalidates a stale disk copy and otherwise relies on LRU eviction under
    // memory pressure -- a project small enough to fit entirely in the RAM budget could otherwise
    // leave nothing on disk for export's own FrameCacheManager instance to find.
    val onSaveProjectClick: (() -> Unit)? = projectId?.let { pid ->
        {
            // Capture intent before queuing behind another save. An earlier toolbar save
            // must not satisfy a later Home request with an older document snapshot.
            val leaveWhenSaved = returnHomeAfterSave
            viewModel.launchSave {
                val time = System.currentTimeMillis()
                val document = try { viewModel.prepareProjectSave(context.resources.displayMetrics.density) } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    projectSaveMessage = error.message ?: "Project was not saved"
                    if (leaveWhenSaved) returnHomeAfterSave = false
                    return@launchSave
                }
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Re-read the manifest first so a save never clobbers the project's own
                    // display name, which the document itself does not carry.
                    val existingManifest = when (val loaded = projectRepository.load(pid)) {
                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> loaded.value.first
                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> null
                    }
                    val manifest = (existingManifest ?: com.donnclab.kinetiframe.data.project.ProjectManifest(
                        projectId = pid, displayName = pid, stage = document.project.stage, fps = document.project.fps,
                        createdAtEpochMs = time, updatedAtEpochMs = time
                    )).copy(stage = document.project.stage, fps = document.project.fps, updatedAtEpochMs = time, totalFrames = document.totalFrames)
                    try {
                        if (packCreatorConfig == null) com.donnclab.kinetiframe.data.project.ProjectFrameDraft.publish(editorCacheDir, File(actorStore.projectDirectory(pid), "frames"))
                    } catch (failure: Exception) {
                        return@withContext com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure(failure.message ?: "Could not save frame pixels", failure)
                    }
                    val saveResult = projectRepository.save(manifest, document)
                    // A fresh gallery thumbnail is worth regenerating on every save, not just the
                    // first one -- it's cheap (one composited frame, reusing the exact same
                    // rendering primitives export uses) next to the win of the gallery always
                    // showing a representative scene rather than an intentional black fade.
                    val thumbnailPath = if (saveResult is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value) {
                        runCatching {
                            val actorRenderer = if (document.project.actorPlacements.isEmpty()) null else {
                                com.donnclab.kinetiframe.domain.render.ActorFrameRenderer(actorRepository, performanceRepository, actorStore)
                            }
                            val backgroundBitmap = document.project.backgroundAssetId
                                ?.let { document.project.assets[it]?.sourcePath }
                                ?.let { path -> android.graphics.BitmapFactory.decodeFile(path) }
                            val compositor = com.donnclab.kinetiframe.domain.render.ProjectFrameCompositor(
                                document.project, document.frameLayers, viewModel.cacheManager, actorRenderer, backgroundBitmap,
                                includeFades = false
                            )
                            try {
                                val posterFrame = com.donnclab.kinetiframe.engine.camera.CameraPosterFrame.select(
                                    document.project.cameraShots, document.totalFrames, document.project.fps)
                                val frame = compositor.composite(posterFrame)
                                val thumbnailFile = projectRepository.thumbnailFile(pid)
                                thumbnailFile.parentFile?.mkdirs()
                                java.io.FileOutputStream(thumbnailFile).use { out ->
                                    frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                                }
                                // compress() above is synchronous and has already read every
                                // pixel out of `frame` by the time control reaches here, so
                                // releasing the compositor's reused bitmaps immediately afterward
                                // is safe -- this renderer/compositor pair is scoped to this one
                                // thumbnail generation only, never shared with the live editor's
                                // own canvas state.
                                thumbnailFile.absolutePath
                            } finally {
                                compositor.release()
                            }
                        }.getOrNull()
                    } else null
                    // Keep the Room gallery index (see data/project/db) in step with an in-editor
                    // save too, not just gallery-side create/rename/delete -- otherwise the index
                    // would only self-heal the next time it happened to come back empty, leaving a
                    // stale duration/timestamp in the gallery until then.
                    if (saveResult is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value) {
                        runCatching {
                            com.donnclab.kinetiframe.data.project.db.AppDatabase.getInstance(context).projectDao().upsert(
                                com.donnclab.kinetiframe.data.project.db.ProjectEntity(
                                    projectId = manifest.projectId, title = manifest.displayName,
                                    createdAtEpochMs = manifest.createdAtEpochMs, lastModified = manifest.updatedAtEpochMs,
                                    durationFrames = manifest.totalFrames, fps = manifest.fps,
                                    resolutionWidth = manifest.stage.width, resolutionHeight = manifest.stage.height,
                                    thumbnailPath = thumbnailPath
                                )
                            )
                        }
                    }
                    saveResult
                }
                projectSaveMessage = when (result) {
                    is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> {
                        frameReferences.clear()
                        if (leaveWhenSaved) { returnHomeAfterSave = false; exitDestination?.invoke() }
                        "Project saved"
                    }
                    is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> {
                        if (leaveWhenSaved) returnHomeAfterSave = false
                        result.message
                    }
                }
            }
        }
    }

    val saveActorClick: (() -> Unit)? = onSaveActorRevision?.let { save ->
        {
            viewModel.launchSave {
                try {
                    viewModel.prepareProjectSave()
                    viewModel.state.value.actorDefinition()?.let { save(it, viewModel.rigAssetManager) }
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    projectSaveMessage = error.message ?: "Actor was not saved"
                }
            }
        }
    }

    // Register one stable action with Actor Studio; read the current canvas at invocation time.
    val actorExitSave by androidx.compose.runtime.rememberUpdatedState<() -> Unit>({
        viewModel.launchSave {
            try {
                viewModel.handleIntent(StudioIntent.ActorMotion.StopTake)
                viewModel.handleIntent(StudioIntent.ActorMotion.KeepTake)
                viewModel.prepareProjectSave()
                val current = viewModel.state.value
                if (current.actor.isPerformanceAuthoring) {
                    val instance = requireNotNull(current.activeActor())
                    requireNotNull(onSavePerformance)(instance, current.actorPreviewDuration())
                } else requireNotNull(onSaveActorRevision)(requireNotNull(current.actorDefinition()), viewModel.rigAssetManager)
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                projectSaveMessage = error.message ?: "Studio work was not saved"
                onActorSavePreparationFailed?.invoke()
            }
        }
    })
    DisposableEffect(Unit) {
        onActorSaveActionAvailable?.invoke { actorExitSave() }
        onDispose { onActorSaveActionAvailable?.invoke(null) }
    }
    fun requestExit(destination: () -> Unit) {
        viewModel.handleIntent(StudioIntent.Playback.Pause)
        if (isActorStudioSession) viewModel.launchSave {
            try {
                viewModel.handleIntent(StudioIntent.ActorMotion.StopTake)
                viewModel.handleIntent(StudioIntent.ActorMotion.KeepTake)
                viewModel.prepareProjectSave()
                onActorStudioDirtyChanged?.invoke(com.donnclab.kinetiframe.ui.actors.hasActorStudioChanges(initialEditorState, viewModel.state.value))
                onExitActorStudio?.invoke()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                projectSaveMessage = error.message ?: "Could not prepare studio exit"
            }
        } else {
            exitDestination = destination
            showExitConfirmation = true
        }
    }
    if (showExitConfirmation) com.donnclab.kinetiframe.ui.components.dialogs.SaveBeforeExitDialog(
        onSave = {
            showExitConfirmation = false
            if (packCreatorConfig != null) scope.launch {
                try { pendingPackSaveFrames = viewModel.prepareProjectSave().totalFrames }
                catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    projectSaveMessage = error.message ?: "Pack was not saved"
                }
            } else { returnHomeAfterSave = true; onSaveProjectClick?.invoke() }
        }, onDiscard = {
            showExitConfirmation = false
            returnHomeAfterSave = false
            exitDestination?.invoke()
        },
        onCancel = { showExitConfirmation = false; exitDestination = null },
        canSave = packCreatorConfig != null || onSaveProjectClick != null)
    BackHandler(enabled = (onBackClick ?: onHomeClick ?: onExitActorStudio) != null) {
        requestExit(requireNotNull(onBackClick ?: onHomeClick ?: onExitActorStudio))
    }

    // The system picker returns a content:// URI. It is decoded to a Bitmap off the main thread
    // here, where the Context is available, and the intent carries the decoded bitmap.
    // BackgroundManager never touches a Context, Uri or ContentResolver.
    val backgroundImagePicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                viewModel.handleIntent(StudioIntent.Background.SetImage(bitmap))
            } else {
                backgroundImageError = "Could not read that image. Try picking a different one."
            }
        }
    }

    val frameImagePicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) scope.launch {
            val bitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { com.donnclab.kinetiframe.data.image.FrameImageDecoder.decode {
                    context.contentResolver.openInputStream(uri)
                } }.getOrNull()
            }
            if (bitmap == null) backgroundImageError = "Could not read that image. Try another file."
            else viewModel.handleIntent(StudioIntent.Selection.StampBitmap(bitmap))
        }
    }
    val thumbnailActorFactory = remember(actorRepository, performanceRepository, actorStore) {
        { com.donnclab.kinetiframe.domain.render.ActorFrameRenderer(actorRepository, performanceRepository, actorStore) }
    }

    var audioError by remember { mutableStateOf<String?>(null) }
    var showRecordAudioDialog by remember { mutableStateOf(false) }
    /** A captured take waiting to be trimmed. Non-null puts the trim screen on screen;
     *  clearing it discards the take (and its temp file) without placing anything. */
    var pendingAudioTake by remember { mutableStateOf<com.donnclab.kinetiframe.ui.components.audio.PendingAudioTake?>(null) }

    /** Reads an audio file's duration without needing Android Context beyond what
     *  MediaMetadataRetriever itself requires -- a plain file path, no ContentResolver. */
    fun readAudioDurationMicros(path: String): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val millis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            millis * 1000L
        } finally {
            retriever.release()
        }
    }

    /** Shared tail of both the import and record flows: a file already sitting in this project's
     *  own `project-assets/audio/` directory becomes a placed clip on the first audio track that
     *  has nothing else on it yet (falling back to track 0 once all six are in use, rather than
     *  refusing -- overlapping clips on one track are allowed, just not encouraged; see
     *  AudioClipPlacement's doc comment). Mirrors BackgroundManager's Context-free contract:
     *  by the time this runs, the file is already on disk and the duration already known. */
    /**
     * [peaks] is the clip's waveform outline. A recording supplies its own for free (the live
     * meter already measured it); an import passes empty and gets decoded once, off the main
     * thread, by the caller -- decoding here would block the tap that placed the clip.
     */
    fun addAudioClip(assetId: String, path: String, durationMicros: Long, label: String, peaks: List<Float> = emptyList()) {
        val fps = state.project.fps
        val durationFrames = kotlin.math.ceil(durationMicros * fps / 1_000_000.0).toInt().coerceAtLeast(1)
        val usedTracks = state.project.audioClips.values.map { it.trackIndex }.toSet()
        val trackIndex = (0 until com.donnclab.kinetiframe.engine.model.MAX_AUDIO_TRACKS).firstOrNull { it !in usedTracks } ?: 0
        val asset = com.donnclab.kinetiframe.engine.model.ProjectAsset(
            id = assetId, kind = com.donnclab.kinetiframe.engine.model.AssetKind.AUDIO, sourcePath = path, width = 0, height = 0
        )
        val placement = com.donnclab.kinetiframe.engine.model.AudioClipPlacement(
            clipId = "audioclip-${java.util.UUID.randomUUID()}", assetId = assetId, trackIndex = trackIndex,
            label = label, startFrame = state.animation.currentFrame, durationFrames = durationFrames,
            waveformPeaks = peaks,
            // An import is placed whole, so its source length and its placed length are the same
            // -- recording it anyway means the trim editor can widen it back later.
            sourceDurationMicros = durationMicros
        )
        viewModel.handleIntent(StudioIntent.Audio.Add(placement, asset))
    }

    /**
     * Places a take using the in/out points chosen on the trim screen.
     *
     * The source file is stored whole and the in-point recorded as trimStartMicros rather than the
     * audio being cut on disk -- trimming stays non-destructive, so widening the clip later reaches
     * real audio instead of silence. [sourcePeaks] is the take's full waveform; only the selected
     * slice is stored on the clip, which is what its lane draws.
     */
    fun addTrimmedAudioClip(
        assetId: String,
        path: String,
        selection: com.donnclab.kinetiframe.engine.audio.AudioTrimSelection,
        sourcePeaks: List<Float>
    ) {
        val asset = com.donnclab.kinetiframe.engine.model.ProjectAsset(
            id = assetId, kind = com.donnclab.kinetiframe.engine.model.AssetKind.AUDIO, sourcePath = path, width = 0, height = 0
        )
        val usedTracks = state.project.audioClips.values.map { it.trackIndex }.toSet()
        val trackIndex = (0 until com.donnclab.kinetiframe.engine.model.MAX_AUDIO_TRACKS).firstOrNull { it !in usedTracks } ?: 0
        val placement = com.donnclab.kinetiframe.engine.model.AudioClipPlacement(
            clipId = "audioclip-${java.util.UUID.randomUUID()}",
            assetId = assetId,
            trackIndex = trackIndex,
            label = "Voice-over",
            startFrame = state.animation.currentFrame,
            durationFrames = selection.toDurationFrames(state.project.fps),
            trimStartMicros = selection.inMicros,
            waveformPeaks = selection.slice(com.donnclab.kinetiframe.engine.audio.Waveform(sourcePeaks)).peaks,
            sourceDurationMicros = selection.sourceDurationMicros
        )
        viewModel.handleIntent(StudioIntent.Audio.Add(placement, asset))
    }

    val audioImportPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val projectAudioDir = File(editorFilesDir, "project-assets/audio").apply { mkdirs() }
                    val assetId = "audio-${java.util.UUID.randomUUID()}"
                    val destination = File(projectAudioDir, "$assetId.audio")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw java.io.IOException("Could not read that audio file")
                    Triple(assetId, destination, readAudioDurationMicros(destination.absolutePath))
                }
            }
            result.fold(
                onSuccess = { (assetId, destination, durationMicros) ->
                    scope.launch {
                        val peaks = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.donnclab.kinetiframe.engine.audio.WaveformExtractor
                                .fromFile(destination.absolutePath, durationMicros).peaks
                        }
                        addAudioClip(assetId, destination.absolutePath, durationMicros, "Imported audio", peaks)
                    }
                },
                onFailure = { audioError = it.message ?: "Could not import that audio file." }
            )
        }
    }

    val recordAudioPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showRecordAudioDialog = true else audioError = "Microphone permission is needed to record audio." }

    fun onRecordAudioClick() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) showRecordAudioDialog = true else recordAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    var videoError by remember { mutableStateOf<String?>(null) }

    /** Imports a video into `project-assets/video/` (never split into frames -- see
     *  VideoClipPlacement's doc comment) and appends it as a new clip right after whatever
     *  already occupies the single video track, via [com.donnclab.kinetiframe.domain.manager.VideoTrackManager.nextFreeStartFrame]
     *  so it's always proposed into genuinely free space. */
    val videoImportPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val projectVideoDir = File(editorFilesDir, "project-assets/video").apply { mkdirs() }
                    val assetId = "video-${java.util.UUID.randomUUID()}"
                    val destination = File(projectVideoDir, "$assetId.video")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw java.io.IOException("Could not read that video file")
                    val retriever = android.media.MediaMetadataRetriever()
                    val metadata = try {
                        retriever.setDataSource(destination.absolutePath)
                        val duration = (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
                        val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                        val hasAudio = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
                        require(duration > 0L && width > 0 && height > 0) { "Could not read video duration or dimensions" }
                        com.donnclab.kinetiframe.data.video.ImportedVideoMetadata(duration, if (rotation % 180 != 0) height else width,
                            if (rotation % 180 != 0) width else height, hasAudio,
                            if (hasAudio) com.donnclab.kinetiframe.engine.audio.WaveformExtractor.fromFile(destination.absolutePath, duration).peaks else emptyList())
                    } finally {
                        retriever.release()
                    }
                    Triple(assetId, destination, metadata)
                }
            }
            result.fold(
                onSuccess = { (assetId, destination, metadata) ->
                    val (durationMicros, sourceWidth, sourceHeight) = metadata
                    val fps = state.project.fps
                    val durationFrames = kotlin.math.ceil(durationMicros * fps / 1_000_000.0).toInt().coerceAtLeast(1)
                    // Next free slot at the tail of the single video track -- mirrors
                    // VideoTrackManager.nextFreeStartFrame() without needing a throwaway manager
                    // instance just to read it.
                    val startFrame = maxOf(state.animation.currentFrame, state.project.videoClips.values.maxOfOrNull { it.timelineEndFrame } ?: 0)
                    val scale = minOf(state.project.stage.width * .5f / sourceWidth, state.project.stage.height * .5f / sourceHeight)
                    val width = sourceWidth * scale
                    val height = sourceHeight * scale
                    val asset = com.donnclab.kinetiframe.engine.model.ProjectAsset(
                        id = assetId, kind = com.donnclab.kinetiframe.engine.model.AssetKind.VIDEO, sourcePath = destination.absolutePath, width = sourceWidth, height = sourceHeight
                    )
                    val audioLane = (0 until com.donnclab.kinetiframe.engine.model.MAX_AUDIO_TRACKS).firstOrNull { state.project.audioTracks[it]?.isLocked != true }
                    val audio = if (metadata.hasAudio && audioLane != null) com.donnclab.kinetiframe.engine.model.AudioClipPlacement(
                        "audioclip-${java.util.UUID.randomUUID()}", assetId, audioLane, "Video sound", startFrame, durationFrames,
                        sourceDurationMicros = durationMicros, waveformPeaks = metadata.peaks) else null
                    val placement = com.donnclab.kinetiframe.engine.model.VideoClipPlacement(
                        clipId = "videoclip-${java.util.UUID.randomUUID()}", assetId = assetId, label = "Imported video",
                        timelineStartFrame = startFrame, timelineEndFrame = startFrame + durationFrames,
                        sourceDurationFrames = durationFrames, isSceneContent = true,
                        x = (state.project.stage.width - width) / 2f, y = (state.project.stage.height - height) / 2f,
                        width = width, height = height, fit = com.donnclab.kinetiframe.engine.model.VideoFit.FIT, linkedAudioClipId = audio?.clipId
                    )
                    viewModel.handleIntent(StudioIntent.Video.Add(placement, asset, audio))
                    if (metadata.hasAudio && audio == null) videoError = "Video added without sound because all audio lanes are locked. Unlock a lane and import again to include sound."
                    viewModel.handleIntent(StudioIntent.Video.Select(placement.clipId))
                },
                onFailure = { videoError = it.message ?: "Could not import that video file." }
            )
        }
    }

    var libraryError by remember { mutableStateOf<String?>(null) }
    // The "Save to: Project / Global" choice is deferred until the user actually picks one -- set
    // by an "Add to Library" action below, rendered as a LibraryScopeDialog near the other alert
    // dialogs, and only then does the real export coroutine (captured here) run.
    var pendingLibraryExport by remember { mutableStateOf<Pair<String, (com.donnclab.kinetiframe.data.actor.LibraryScope) -> Unit>?>(null) }
    var libraryMessage by remember { mutableStateOf<String?>(null) }
    var showLibraryPicker by remember { mutableStateOf(false) }
    var artworkPartId by remember { mutableStateOf<String?>(null) }
    var showActorSketch by remember { mutableStateOf(false) }
    var openingActorSketch by remember { mutableStateOf(false) }
    fun openActorSketch() {
        if (openingActorSketch) return
        openingActorSketch = true
        viewModel.launchSave {
            try {
                viewModel.prepareProjectSave()
                viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                showActorSketch = true
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                projectSaveMessage = failure.message ?: "Could not open drawing"
            } finally { openingActorSketch = false }
        }
    }
    // A whole-surface source must never silently receive selected-part raster strokes.
    LaunchedEffect(state.actorDefinition()?.sceneGraph?.drawing?.enabled, state.scene.studioMode) {
        if (isActorStudioSession && !state.actor.isPerformanceAuthoring &&
            state.actorDefinition()?.sceneGraph?.drawing?.enabled == true && state.scene.studioMode == StudioMode.RASTER_DRAWING) {
            openActorSketch()
        }
    }
    if (showActorSketch) state.actorDefinition()?.let { definition ->
        com.donnclab.kinetiframe.ui.actors.sketch.ActorSketchEditor(
            definition, state.project.stage.width, state.project.stage.height,
            viewModel.rigAssetManager, definition.sceneGraph.drawing?.inkAssetId,
            viewModel::handleIntent, { showActorSketch = false }
        )
        timelineError?.let { message ->
            com.donnclab.kinetiframe.ui.components.dialogs.EditorErrorDialog(message) { timelineError = null }
        }
        return
    }
    var artworkAsVariant by remember { mutableStateOf(false) }
    var replacementArtwork by remember { mutableStateOf<String?>(null) }
    var editingArtwork by remember { mutableStateOf<Pair<String, String>?>(null) }
    val chooseActorArtwork: (Boolean) -> Unit = { asVariant ->
        replacementArtwork = null
        state.scene.selectedBoneId?.let { part ->
            viewModel.handleIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
            artworkAsVariant = asVariant
            artworkPartId = part
        }
    }
    artworkPartId?.let { part -> state.actorDefinition()?.sceneGraph?.let { graph ->
        com.donnclab.kinetiframe.ui.actors.ActorArtworkAttachmentFlow(part, graph, libraryRepository,
            context.cacheDir, { artworkPartId = null }, viewModel::handleIntent, initialAsVariant = artworkAsVariant,
            replaceVariantId = replacementArtwork, assets = viewModel.rigAssetManager)
    } }
    editingArtwork?.let { (part, variant) -> state.actorDefinition()?.sceneGraph?.bones?.get(part)?.let { bone ->
        com.donnclab.kinetiframe.ui.actors.ActorArtworkEditor(bone, variant, viewModel.rigAssetManager,
            { editingArtwork = null }, viewModel::handleIntent, graph = state.actorDefinition()?.sceneGraph)
    } }

    // Tapping a PACK in the Library picker sets this instead of routing through applyLibraryPick,
    // and this effect fetches + decodes every one of that pack's drawings once so PackTray can
    // show and drag them without re-hitting disk per
    // drag. packDragItem/packDragPosition track one in-flight drag at a time, in the same
    // on-screen-pixel space as [canvasRootPositionInWindow] (captured on the Box that wraps
    // CanvasArea below) -- see applyPackDrop's own comment for how a drop point becomes a stamp.
    var packTrayManifest by remember { mutableStateOf<com.donnclab.kinetiframe.data.actor.LibraryItemManifest?>(null) }
    var packTrayItems by remember { mutableStateOf<List<com.donnclab.kinetiframe.ui.packs.PackTrayItem>>(emptyList()) }
    var packTrayLoading by remember { mutableStateOf(false) }
    var packTrayError by remember { mutableStateOf<String?>(null) }
    var packDragItem by remember { mutableStateOf<com.donnclab.kinetiframe.ui.packs.PackTrayItem?>(null) }
    var packDragPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasRootPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(packTrayManifest) {
        val manifest = packTrayManifest ?: return@LaunchedEffect
        packTrayLoading = true
        packTrayError = null
        packTrayItems = emptyList()
        val projectIdForScope = if (manifest.scope == com.donnclab.kinetiframe.data.actor.LibraryScope.PROJECT) state.project.id else null
        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val (_, packManifest) = when (val loaded = libraryRepository.loadPack(manifest.libraryItemId, manifest.scope, projectIdForScope)) {
                    is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> loaded.value
                    is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
                }
                packManifest.items.map { entry ->
                    val destination = File(context.cacheDir, "pack-tray-${java.util.UUID.randomUUID()}.png")
                    when (val copied = libraryRepository.copyPackItemTo(manifest.libraryItemId, entry.file, destination, manifest.scope, projectIdForScope)) {
                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> Unit
                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> throw IllegalStateException(copied.message, copied.cause)
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeFile(destination.absolutePath)
                        ?: throw java.io.IOException("Could not read \"${entry.name}\"")
                    com.donnclab.kinetiframe.ui.packs.PackTrayItem(entry.file, entry.name, bitmap)
                }
            }
        }
        packTrayLoading = false
        result.fold(onSuccess = { packTrayItems = it }, onFailure = { packTrayError = it.message ?: "Could not open this pack" })
    }

    /** Turns a finished drag's last known screen position into a stage-space stamp -- inverts the
     *  canvas's own current pan/zoom matrix (the same [state.canvas.matrixValues] CanvasArea's
     *  Canvas concats onto itself) so a drop lands under the finger regardless of zoom level, and
     *  drops the stamp silently (no error, just no-op) if the point falls outside the stage, since
     *  "missed the canvas" isn't a mistake worth interrupting the animator over. */
    fun applyPackDrop() {
        val item = packDragItem
        val position = packDragPosition
        packDragItem = null
        packDragPosition = null
        if (item == null || position == null) return
        val local = position - canvasRootPositionInWindow
        val forward = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) }
        val inverse = android.graphics.Matrix()
        if (!forward.invert(inverse)) return
        val points = floatArrayOf(local.x, local.y)
        inverse.mapPoints(points)
        val stageX = points[0]
        val stageY = points[1]
        if (stageX < 0f || stageY < 0f || stageX > state.project.stage.width || stageY > state.project.stage.height) return
        viewModel.handleIntent(StudioIntent.Selection.StampBitmap(item.thumbnail, stageX, stageY))
    }

    /** What the in-editor Add menu's "From Library" item can pick from.
     *  PROJECT is deliberately excluded -- importing a whole other project while one is already
     *  open doesn't make sense, that only belongs on the gallery's own import action. BUNDLE is
     *  excluded too: no screen creates BUNDLE items yet, so offering them would lead nowhere. FRAME stamps its
     *  picked drawing onto the current frame via [StudioIntent.Selection.StampBitmap], centered,
     *  the moment it's tapped. PACK never reaches [applyLibraryPick] at all -- tapping one opens
     *  [com.donnclab.kinetiframe.ui.packs.PackTray] instead (see [packTrayManifest] below), since a pack's
     *  own usage model is "drag any item out onto the canvas, repeatedly," not "tap once and
     *  close." */
    val editorLibraryKinds = remember { setOf(
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.ACTOR,
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.PERFORMANCE,
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.AUDIO,
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.BACKGROUND_IMAGE,
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.FRAME,
        com.donnclab.kinetiframe.data.actor.LibraryItemKind.PACK
    ) }

    /** Copies a raw-asset library item's payload onto local disk at [destination]. */
    suspend fun fetchLibraryPayload(
        manifest: com.donnclab.kinetiframe.data.actor.LibraryItemManifest,
        destination: File,
        library: com.donnclab.kinetiframe.data.library.LibraryRepository
    ) {
        destination.parentFile?.mkdirs()
        // manifest.scope/sourceProjectId is self-describing -- the picker only ever browses either
        // the PERSONAL library or the currently-open project's own PROJECT vault, so the item's own
        // recorded scope always resolves back to exactly this project when it says PROJECT.
        val projectIdForScope = if (manifest.scope == com.donnclab.kinetiframe.data.actor.LibraryScope.PROJECT) state.project.id else null
        when (val result = library.copyAssetTo(manifest.libraryItemId, destination, manifest.scope, projectIdForScope)) {
            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> Unit
            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
    }

    /** Routes a picked library item to the right track by its kind -- the manifest
     *  itself says where it belongs, so this is a single smart-routing Add path. Every branch
     *  below reuses the exact same import machinery an
     *  ordinary import/record already uses (addAudioClip, StudioIntent.Background.SetImage,
     *  ActorBundleRepository) rather than a second, parallel path. */
    fun applyLibraryPick(manifest: com.donnclab.kinetiframe.data.actor.LibraryItemManifest) {
        showLibraryPicker = false
        scope.launch {
            val time = System.currentTimeMillis()
            val outcome = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    when (manifest.kind) {
                        com.donnclab.kinetiframe.data.actor.LibraryItemKind.AUDIO -> {
                            val projectAudioDir = File(editorFilesDir, "project-assets/audio").apply { mkdirs() }
                            val destination = File(projectAudioDir, "audio-${java.util.UUID.randomUUID()}.audio")
                            fetchLibraryPayload(manifest, destination, libraryRepository)
                            val durationMicros = readAudioDurationMicros(destination.absolutePath)
                            "audio" to Triple(destination.absolutePath, durationMicros, manifest.displayName)
                        }
                        com.donnclab.kinetiframe.data.actor.LibraryItemKind.BACKGROUND_IMAGE -> {
                            val destination = File(context.cacheDir, "library-bg-${java.util.UUID.randomUUID()}")
                            fetchLibraryPayload(manifest, destination, libraryRepository)
                            val bitmap = android.graphics.BitmapFactory.decodeFile(destination.absolutePath)
                                ?: throw java.io.IOException("Could not read that image")
                            "background" to bitmap
                        }
                        com.donnclab.kinetiframe.data.actor.LibraryItemKind.FRAME -> {
                            val destination = File(context.cacheDir, "library-frame-stamp-${java.util.UUID.randomUUID()}.png")
                            fetchLibraryPayload(manifest, destination, libraryRepository)
                            val bitmap = android.graphics.BitmapFactory.decodeFile(destination.absolutePath)
                                ?: throw java.io.IOException("Could not read that frame")
                            "stamp" to bitmap
                        }
                        com.donnclab.kinetiframe.data.actor.LibraryItemKind.ACTOR -> {
                            val newActorId = "actor-${java.util.UUID.randomUUID()}"
                            val newRevisionId = "revision-${java.util.UUID.randomUUID()}"
                            when (val imported = actorBundles.importActor(manifest.libraryItemId, newActorId, newRevisionId, time)) {
                                is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "actor" to imported.value.displayName
                                is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> throw IllegalStateException(imported.message, imported.cause)
                            }
                        }
                        com.donnclab.kinetiframe.data.actor.LibraryItemKind.PERFORMANCE -> {
                            val newPerformanceId = "performance-${java.util.UUID.randomUUID()}"
                            when (val imported = actorBundles.importPerformance(manifest.libraryItemId, state.project.id, newPerformanceId, time)) {
                                is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "performance" to imported.value
                                is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> throw IllegalStateException(imported.message, imported.cause)
                            }
                        }
                        else -> throw IllegalStateException("This item can't be added from here yet")
                    }
                }
            }
            outcome.fold(
                onSuccess = { (kind, payload) ->
                    when (kind) {
                        "audio" -> {
                            val (path, durationMicros, label) = payload as Triple<String, Long, String>
                            addAudioClip("audio-${java.util.UUID.randomUUID()}", path, durationMicros, label)
                        }
                        "background" -> viewModel.handleIntent(StudioIntent.Background.SetImage(payload as android.graphics.Bitmap))
                        "stamp" -> viewModel.handleIntent(StudioIntent.Selection.StampBitmap(payload as android.graphics.Bitmap))
                        "actor" -> libraryMessage = "Added \"$payload\" to your actors"
                        "performance" -> {
                            val performance = payload as com.donnclab.kinetiframe.data.actor.PerformanceDocument
                            val durationFrames = durationFramesFor(performance, state.project.fps)
                            placeActor(com.donnclab.kinetiframe.engine.model.ActorPlacementReference(
                                "placement-${java.util.UUID.randomUUID()}", performance.performanceId, performance.actorId,
                                performance.actorRevisionId, performance.displayName, state.animation.currentFrame,
                                durationFrames, zIndex = state.animation.currentFrame
                            ))
                        }
                    }
                },
                onFailure = { libraryError = it.message ?: "Could not add that item" }
            )
        }
    }

    if (textAddDialogState != null) {
        val (x, y) = textAddDialogState!!
        TextInsertionDialog(
            onDismiss = { textAddDialogState = null },
            onConfirm = { text ->
                viewModel.handleIntent(StudioIntent.Text.AddText(text, x, y))
                textAddDialogState = null
            }
        )
    }

    if (textEditDialogState != null) {
        val (id, initialText) = textEditDialogState!!
        TextInsertionDialog(
            initialText = initialText,
            onDismiss = { textEditDialogState = null },
            onConfirm = { text ->
                viewModel.handleIntent(StudioIntent.Text.UpdateText(id, text))
                textEditDialogState = null
            }
        )
    }

    actorLibraryMessage?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { actorLibraryMessage = null },
        title = { androidx.compose.material3.Text("Actor library") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { actorLibraryMessage = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    backgroundImageError?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { backgroundImageError = null },
        title = { androidx.compose.material3.Text("Background image") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { backgroundImageError = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    timelineError?.let { message ->
        com.donnclab.kinetiframe.ui.components.dialogs.EditorErrorDialog(message) { timelineError = null }
    }

    projectSaveMessage?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { projectSaveMessage = null },
        title = { androidx.compose.material3.Text("Save Project") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { projectSaveMessage = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    audioError?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { audioError = null },
        title = { androidx.compose.material3.Text("Audio") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { audioError = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    videoError?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { videoError = null },
        title = { androidx.compose.material3.Text("Video") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { videoError = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    libraryError?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { libraryError = null },
        title = { androidx.compose.material3.Text("Library") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { libraryError = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    libraryMessage?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { libraryMessage = null },
        title = { androidx.compose.material3.Text("Library") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { libraryMessage = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    pendingLibraryExport?.let { (label, action) ->
        com.donnclab.kinetiframe.ui.library.LibraryScopeDialog(
            itemLabel = label,
            onDismiss = { pendingLibraryExport = null },
            onChoose = { chosenScope -> pendingLibraryExport = null; action(chosenScope) }
        )
    }

    if (showLibraryPicker) {
        com.donnclab.kinetiframe.ui.library.LibraryPickerDialog(
            library = libraryRepository,
            allowedKinds = editorLibraryKinds,
            projectId = state.project.id,
            onDismiss = { showLibraryPicker = false },
            onOpenPack = { manifest -> packTrayManifest = manifest },
            onPick = ::applyLibraryPick
        )
    }

    packTrayError?.let { message -> androidx.compose.material3.AlertDialog(
        onDismissRequest = { packTrayError = null; packTrayManifest = null },
        title = { androidx.compose.material3.Text("Pack") },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { packTrayError = null; packTrayManifest = null }) {
            androidx.compose.material3.Text("OK")
        } }
    ) }

    if (showRecordAudioDialog) {
        // The recorder owns a native MediaRecorder, so it is remembered alongside the dialog and
        // released on dispose -- see VoiceRecorder for why that lifecycle left the composable.
        val voiceRecorder = remember { com.donnclab.kinetiframe.domain.manager.VoiceRecorder(context, scope) }
        val recorderState by voiceRecorder.state.collectAsStateWithLifecycle()
        val takeFile = remember { File(context.cacheDir, "voiceover-${System.currentTimeMillis()}.m4a") }
        DisposableEffect(Unit) { onDispose { voiceRecorder.release() } }

        Dialog(onDismissRequest = { voiceRecorder.release(); showRecordAudioDialog = false }) {
            AudioRecorderSheet(
                state = recorderState,
                actions = AudioRecorderActions(
                    onRecord = { voiceRecorder.start(takeFile) },
                    onStop = { voiceRecorder.stop() },
                    onDiscard = { voiceRecorder.cancel() },
                    onUse = {
                        // Hands off to the trim screen rather than placing straight away: length
                        // is decided before the clip lands, so it never has to be re-dragged back
                        // into sync after being tightened. Peaks are grabbed before release()
                        // clears recorder state -- they are the exact take just watched.
                        val take = com.donnclab.kinetiframe.ui.components.audio.PendingAudioTake(
                            file = takeFile,
                            durationMicros = recorderState.elapsedMicros,
                            peaks = recorderState.waveform.peaks
                        )
                        voiceRecorder.release()
                        showRecordAudioDialog = false
                        pendingAudioTake = take
                    }
                ),
                onClose = { voiceRecorder.release(); showRecordAudioDialog = false }
            )
        }
    }

    pendingAudioTake?.let { take ->
        val previewPlayer = remember(take) { com.donnclab.kinetiframe.domain.manager.TrimPreviewPlayer(scope) }
        val previewState by previewPlayer.state.collectAsStateWithLifecycle()
        // A take shorter than the minimum selection cannot be trimmed meaningfully; guard the
        // selection's own requirement rather than letting it throw on construction.
        val sourceDuration = take.durationMicros.coerceAtLeast(
            com.donnclab.kinetiframe.engine.audio.AudioTrimSelection.MIN_SELECTION_MICROS
        )
        var selection by remember(take) {
            mutableStateOf(com.donnclab.kinetiframe.engine.audio.AudioTrimSelection(sourceDuration))
        }
        DisposableEffect(take) { onDispose { previewPlayer.release() } }

        fun dismissTake() {
            previewPlayer.release()
            runCatching { take.file.delete() }
            pendingAudioTake = null
        }

        Dialog(onDismissRequest = { dismissTake() }) {
            com.donnclab.kinetiframe.ui.components.audio.AudioTrimScreen(
                state = com.donnclab.kinetiframe.ui.components.audio.AudioTrimState(
                    waveform = com.donnclab.kinetiframe.engine.audio.Waveform(take.peaks),
                    selection = selection,
                    playheadMicros = previewState.positionMicros,
                    isPreviewing = previewState.isPlaying
                ),
                actions = com.donnclab.kinetiframe.ui.components.audio.AudioTrimActions(
                    onSelectionChange = { updated ->
                        selection = updated
                        // Dragging a handle invalidates what is currently sounding, so the
                        // preview stops rather than continuing to play the old range.
                        if (previewState.isPlaying) previewPlayer.stop()
                    },
                    onTogglePreview = {
                        if (previewState.isPlaying) previewPlayer.stop()
                        else previewPlayer.play(take.file.absolutePath, selection)
                    },
                    onConfirm = {
                        previewPlayer.release()
                        val committed = selection
                        pendingAudioTake = null
                        scope.launch {
                            val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching {
                                    val projectAudioDir = File(editorFilesDir, "project-assets/audio").apply { mkdirs() }
                                    val assetId = "audio-${java.util.UUID.randomUUID()}"
                                    val destination = File(projectAudioDir, "$assetId.m4a")
                                    take.file.copyTo(destination, overwrite = true)
                                    take.file.delete()
                                    assetId to destination
                                }
                            }
                            result.fold(
                                onSuccess = { (assetId, destination) ->
                                    addTrimmedAudioClip(assetId, destination.absolutePath, committed, take.peaks)
                                },
                                onFailure = { audioError = it.message ?: "Could not save the recording." }
                            )
                        }
                    },
                    onCancel = { dismissTake() }
                )
            )
        }
    }

    if (frameStudioVisible) {
        FrameStudio(
            actorRendererFactory = thumbnailActorFactory,
            state = state,
            cacheManager = viewModel.cacheManager,
            onIntent = viewModel::handleIntent,
            onOpenFrame = { frame ->
                viewModel.handleIntent(StudioIntent.Playback.SetCurrentFrame(frame))
                frameStudioVisible = false
            },
            onDismiss = { frameStudioVisible = false }
        )
    }

    if (layerSheetVisible) {
        LayerPanelPopup(
            state = state,
            onIntent = viewModel::handleIntent,
            cacheManager = viewModel.cacheManager,
            onDismiss = { layerSheetVisible = false }
        )
    }

    if (showGridMenu) {
        GridOptionsPopup(
            state = state,
            onIntent = viewModel::handleIntent,
            onDismiss = { showGridMenu = false }
        )
    }

    if (showOnionMenu) {
        OnionOptionsPopup(
            state = state,
            onIntent = viewModel::handleIntent,
            onDismiss = { showOnionMenu = false }
        )
    }

    BackHandler(drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    val timelineAssetPicker: @Composable () -> Unit = {
        ActorPerformancePicker(state.project.id, state.project.fps, state.animation.currentFrame, actorRepository,
            performanceRepository, onPlace = { tracksWorkspaceVisible = false; placeActor(it) },
            onCreatePerformance = onCreatePerformance,
            onImportAudio = { tracksWorkspaceVisible = false; audioImportPicker.launch("audio/*") },
            onRecordAudio = { tracksWorkspaceVisible = false; onRecordAudioClick() },
            onImportVideo = { tracksWorkspaceVisible = false; videoImportPicker.launch("video/*") },
            onFromLibrary = { tracksWorkspaceVisible = false; showLibraryPicker = true })
    }
    packItemDetails?.let { (index, creating) ->
        packCreatorConfig?.let { config ->
            com.donnclab.kinetiframe.ui.packs.PackItemDialog(if (creating) com.donnclab.kinetiframe.ui.packs.PackFrameTag() else state.packTags[index] ?: com.donnclab.kinetiframe.ui.packs.PackFrameTag(),
                creating, onDismiss = { packItemDetails = null }, onSave = { tag ->
                    if (creating && state.animation.totalFrames >= 1000) {
                        timelineError = "Maximum item limit reached"
                    } else {
                        viewModel.handleIntent(if (creating) StudioIntent.PackItem.Add(index, tag) else StudioIntent.PackItem.SetTag(index, tag))
                    }
                    packItemDetails = null
                })
        }
    }
    pendingPackSaveFrames?.let { count ->
        packCreatorConfig?.let { config ->
            com.donnclab.kinetiframe.ui.packs.PackSaveTagsDialog(count, state.packTags, state.animation.currentFrame,
                onDismiss = { pendingPackSaveFrames = null }, onSave = { tags ->
                    config.onReplaceTags(tags)
                    pendingPackSaveFrames = null
                    config.onSavePack(count)
                })
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                LayerPanel(
                    state = state,
                    onIntent = viewModel::handleIntent,
                    onLayerClick = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isPuppetStudio) {
                    PuppetTopBar(viewModel::handleIntent, onInspectorToggle = {
                        if (state.scene.studioMode == StudioMode.RIG_SETUP) showPartPicker = true else showPresetLibrary = true
                    },
                        studioMode = state.scene.studioMode,
                        skeletonOverlayVisible = state.scene.skeletonOverlayVisible,
                        performanceOnly = state.actor.isPerformanceAuthoring,
                        canUndo = state.actor.canUndo, canRedo = state.actor.canRedo,
                        onSave = saveActorClick?.takeIf { state.scene.studioMode == StudioMode.RIG_SETUP },
                        onSavePerformance = onSavePerformance?.takeIf { state.scene.studioMode == StudioMode.ANIMATE_IK &&
                            state.actor.takePhase == PerformanceTakePhase.IDLE }?.let { save ->
                            { state.activeActor()?.let { instance -> save(instance, state.actorPreviewDuration()) } }
                        }, onPerformanceActions = onPerformanceActions?.takeIf { state.scene.studioMode == StudioMode.ANIMATE_IK },
                        onExit = onExitActorStudio?.let { exit -> { requestExit(exit) } }, actorViews = actorViews,
                        onAttachArtwork = chooseActorArtwork.takeIf { isActorStudioSession },
                        hasSelectedPart = state.scene.selectedBoneId != null)
                } else if (toolbarVisible && (!state.animation.isPlaying || !playbackHidesTools)) {
                    EditorTopBar(
                        onFocusCanvas = { toolbarVisible = false },
                        onTracksOpen = if (packCreatorConfig == null && !isActorStudioSession) ({ tracksWorkspaceVisible = true }) else null,
                        onEditPackItem = packCreatorConfig?.let { { packItemDetails = state.animation.currentFrame to false } },
                        state = state,
                        onIntent = viewModel::handleIntent,
                        actorViews = actorViews,
                        onAttachActorArtwork = chooseActorArtwork.takeIf { isActorStudioSession && !state.actor.isPerformanceAuthoring },
                        onHomeClick = (onHomeClick ?: onExitActorStudio)?.let { home -> { requestExit(home) } },
                        onStudioClick = { frameStudioVisible = true },
                        onGridSettingsClick = { showGridMenu = true },
                        onOnionSettingsClick = { showOnionMenu = true },
                        // Actor Studio is reached as its own standalone destination now (see
                        // MainActivity's ActorStudioKey entry); a plain project's editor never navigates there --
                        // this only ever toggles Rig Setup mode in place, and only shows at all
                        // inside the Actor Studio's own instance of this screen (see
                        // EditorTopBar's isActorStudio gating).
                        isActorStudio = initialEditorState != null,
                        onSaveActorClick = saveActorClick,
                        showOverflowMenu = showOverflowMenu,
                        onOverflowMenuToggle = { showOverflowMenu = it },
                        onSetBackgroundClick = { backgroundImagePicker.launch("image/*") },
                        onImportFrameImage = if (initialEditorState == null) ({ frameImagePicker.launch("image/*") }) else null,
                        onSaveProjectClick = onSaveProjectClick,
                        onAddBackgroundToLibraryClick = {
                            pendingLibraryExport = "this background image" to { chosenScope ->
                                scope.launch {
                                    val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        runCatching {
                                            val sourcePath = state.project.backgroundAssetId
                                                ?.let { state.project.assets[it]?.sourcePath }
                                                ?: throw IllegalStateException("No background image is set")
                                            libraryRepository.exportAsset(
                                                kind = com.donnclab.kinetiframe.data.actor.LibraryItemKind.BACKGROUND_IMAGE,
                                                itemId = "library-${java.util.UUID.randomUUID()}",
                                                displayName = "Background",
                                                sourceFile = File(sourcePath),
                                                scope = chosenScope, projectId = state.project.id,
                                                nowEpochMs = System.currentTimeMillis()
                                            )
                                        }.getOrElse { com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure(it.message ?: "Could not add the background to your library", it) }
                                    }
                                    libraryMessage = when (result) {
                                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "Added background image to your library"
                                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> result.message
                                    }
                                }
                            }
                        },
                        onAddCurrentFrameToLibraryClick = {
                            val frame = state.currentBitmap
                            if (frame == null) {
                                libraryError = "Nothing to save -- this frame is empty"
                            } else {
                                pendingLibraryExport = "this frame" to { chosenScope ->
                                    scope.launch {
                                        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            runCatching {
                                                val tempFile = File(context.cacheDir, "library-frame-${java.util.UUID.randomUUID()}.png")
                                                tempFile.outputStream().use { out -> frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
                                                libraryRepository.exportAsset(
                                                    kind = com.donnclab.kinetiframe.data.actor.LibraryItemKind.FRAME,
                                                    itemId = "library-${java.util.UUID.randomUUID()}",
                                                    displayName = "Frame ${state.animation.currentFrame + 1}",
                                                    sourceFile = tempFile,
                                                    scope = chosenScope, projectId = state.project.id,
                                                    nowEpochMs = System.currentTimeMillis()
                                                )
                                            }.getOrElse { com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure(it.message ?: "Could not add that frame to your library", it) }
                                        }
                                        libraryMessage = when (result) {
                                            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "Added frame to your library"
                                            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> result.message
                                        }
                                    }
                                }
                            }
                        },
                        // Pack Creator reads frames back through its own short-lived FrameCacheManager
                        // instance (see PackCreatorRoute.savePack) -- a plain cache put here only
                        // invalidates a stale disk copy, so the frame this session just drew can still
                        // be RAM-only. Flush it to disk first so that reader actually finds it instead
                        // of reporting the frame as undrawn.
                        onSavePackClick = packCreatorConfig?.onSavePack?.let { onSavePack ->
                            {
                                scope.launch {
                                    val saved = try { viewModel.prepareProjectSave() } catch (error: Exception) {
                                        if (error is kotlinx.coroutines.CancellationException) throw error
                                        projectSaveMessage = error.message ?: "Pack was not saved"
                                        return@launch
                                    }
                                    // Pack Creator cannot read a stale scratch-document frame
                                    // count here: it has to export the timeline the animator is
                                    // looking at, including frames added during this session.
                                    pendingPackSaveFrames = saved.totalFrames
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isPuppetStudio && toolbarVisible) {
                    // One LazyListState shared by the track lanes and the frame filmstrip below
                    // them, so scrolling either one keeps the other in lockstep (TimelineScrollSync
                    // handles the syncing).
                    // The Scaffold's own contentWindowInsets is zeroed out (see below) so the
                    // drawing canvas can stay edge-to-edge; that also means nothing reacts to the
                    // on-screen keyboard by default. Pack Creator's inline Name/Description row
                    // lives in this bottom bar, so it needs its own ime padding or the keyboard
                    // just covers whichever field is focused.
                    Column(Modifier.fillMaxWidth().imePadding()) {
                    // Pack Creator drafts are pure raster drawing -- no actor/audio/video tracks
                    // or the "+" Add menu that places them, so this whole section is skipped there.
                    if (packCreatorConfig == null && tracksWorkspaceVisible) {
                        com.donnclab.kinetiframe.ui.components.timeline.TracksWorkspaceDialog(
                            isPlaying = state.animation.isPlaying,
                            onIntent = viewModel::handleIntent,
                            onDismiss = { tracksWorkspaceVisible = false },
                            assetPicker = timelineAssetPicker
                        ) {
                        TimelineTracksPanel(
                            workspaceMode = true,
                            state = state,
                            onIntent = viewModel::handleIntent,
                            performanceRepository = performanceRepository,
                            onEditPlacement = { placement -> onEditPerformance?.invoke(placement, state.project.id) },
                            onCreatePerformance = onCreatePerformance,
                            filmstripScrollState = timelineScrollState,
                            onAddToLibrary = { placement ->
                                scope.launch {
                                    val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        actorBundles.exportPerformance(
                                            state.project.id, placement.performanceId,
                                            "library-${java.util.UUID.randomUUID()}", placement.label,
                                            System.currentTimeMillis()
                                        )
                                    }
                                    actorLibraryMessage = when (result) {
                                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "Added ${placement.label} to your library"
                                        is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> result.message
                                    }
                                }
                            },
                            onAddAudioToLibrary = { clip ->
                                pendingLibraryExport = "\"${clip.label}\"" to { chosenScope ->
                                    scope.launch {
                                        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            runCatching {
                                                val sourcePath = state.project.assets[clip.assetId]?.sourcePath
                                                    ?: throw IllegalStateException("Could not find that audio clip's file")
                                                libraryRepository.exportAsset(
                                                    kind = com.donnclab.kinetiframe.data.actor.LibraryItemKind.AUDIO,
                                                    itemId = "library-${java.util.UUID.randomUUID()}",
                                                    displayName = clip.label,
                                                    sourceFile = File(sourcePath),
                                                    scope = chosenScope, projectId = state.project.id,
                                                    nowEpochMs = System.currentTimeMillis()
                                                )
                                            }.getOrElse { com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure(it.message ?: "Could not add that clip to your library", it) }
                                        }
                                        libraryMessage = when (result) {
                                            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Value -> "Added \"${clip.label}\" to your library"
                                            is com.donnclab.kinetiframe.data.actor.ActorStoreResult.Failure -> result.message
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth())
                        }
                    }
                        TimelineControls(
                            actorRendererFactory = thumbnailActorFactory,
                            state = state,
                            onIntent = { intent ->
                                when {
                                    packCreatorConfig != null && intent is StudioIntent.Frame.AddFrameAt -> packItemDetails = intent.index to true
                                    packCreatorConfig != null && intent == StudioIntent.Frame.AddFrame -> packItemDetails = (state.animation.currentFrame + 1) to true
                                    else -> viewModel.handleIntent(intent)
                                }
                            },
                            cacheManager = viewModel.cacheManager,
                            onLayerPanelToggle = { layerSheetVisible = !layerSheetVisible },
                            listState = timelineScrollState,
                            packMode = packCreatorConfig != null,
                            onAddPackItem = packCreatorConfig?.let { { packItemDetails = state.animation.totalFrames to true } },
                            onEditPackItem = packCreatorConfig?.let { { index -> packItemDetails = index to false } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // The reference frame every pack-drag coordinate is measured against -- both
                    // CanvasArea and PackTray fill this same Box with no extra offset of their
                    // own, so subtracting this position turns a drag's window-space point into
                    // exactly the local pixel space CanvasArea's own matrixValues transform draws
                    // in (see applyPackDrop above).
                    .onGloballyPositioned { coordinates -> canvasRootPositionInWindow = coordinates.positionInWindow() }
            ) {
                CanvasArea(
                    cameraPreview = !isPuppetStudio && packCreatorConfig == null && (cameraPreview || state.animation.isPlaying),
                    actorReference = actorReference ?: frameReference,
                    state = state,
                    onIntent = viewModel::handleIntent,
                    modifier = Modifier.fillMaxSize(),
                    cacheManager = viewModel.cacheManager,
                    rigAssetManager = viewModel.rigAssetManager,
                    actorFiles = actorStore,
                    actorRepository = actorRepository,
                    performanceRepository = performanceRepository,
                    projectAssetManager = viewModel.projectAssetManager,
                    videoFrameProvider = viewModel.videoFrameProvider
                )
                if (!isPuppetStudio && packCreatorConfig == null) {
                    com.donnclab.kinetiframe.ui.components.camera.CameraWorkspaceControls(state, cameraPreview,
                        { cameraPreview = it }, viewModel::handleIntent,
                        Modifier.align(Alignment.TopEnd).padding(8.dp)) { pose, shot, frame ->
                        val previewState = if (shot == null || frame == null) state else state.copy(
                            project = state.project.copy(cameraShots = state.project.cameraShots.filter { it.id != shot.id && it.endFrame <= shot.startFrame } + shot),
                            animation = state.animation.copy(currentFrame = frame, playbackFrame = frame, isPlaying = true))
                        val latestPreview by androidx.compose.runtime.rememberUpdatedState(previewState)
                        // The dialog owns its decoder. Closing it must not leave the real
                        // workspace holding a decoded video frame from the preview clock.
                        val previewVideo = remember { com.donnclab.kinetiframe.domain.manager.VideoFrameProvider { latestPreview } }
                        DisposableEffect(previewVideo) { onDispose { previewVideo.release() } }
                        CanvasArea(state = previewState, onIntent = {}, cameraPreview = true,
                            cameraPoseOverride = if (shot?.action == com.donnclab.kinetiframe.engine.camera.CameraAction.FOLLOW) null else pose, fitStage = true,
                            cacheManager = viewModel.cacheManager, actorFiles = actorStore,
                            actorRepository = actorRepository, performanceRepository = performanceRepository,
                            projectAssetManager = viewModel.projectAssetManager, videoFrameProvider = previewVideo)
                    }
                }
                if (!toolbarVisible && !isPuppetStudio) {
                    com.donnclab.kinetiframe.ui.components.timeline.StudioFocusControls(
                        state.animation.isPlaying, viewModel::handleIntent,
                        onRestore = { toolbarVisible = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    )
                }

                // A faint, non-rendering crosshair anchor so a pack's drawings line up across
                // frames (e.g. an angry eye's pupil sitting at the same spot as a winking eye's).
                // Purely an overlay: never composited into the frame bitmap, never intercepts touch.
                if (packCreatorConfig != null) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val strokeColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f)
                        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(size.width / 2f, 0f), androidx.compose.ui.geometry.Offset(size.width / 2f, size.height), strokeWidth = 2f)
                        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(0f, size.height / 2f), androidx.compose.ui.geometry.Offset(size.width, size.height / 2f), strokeWidth = 2f)
                    }
                }

                // The part rail is "which body part am I working on", which is a different
                // question from "which tool am I holding" -- so it stays on the canvas for BOTH
                // authoring tools. Gating it to RIG_SETUP (the pivot-adjust tool) meant the rail
                // vanished the instant the animator picked up the Pen to actually sketch, forcing
                // a Bone -> tap part -> Pen -> paint -> Bone -> tap next part dance, with the
                // target selector invisible exactly while it was being used. The blueprint's
                // sketch flow is a persistent slot selection the brush paints into, so the rail
                // is shown whenever an actor is being authored and we are not posing.
                // PartActionsRow stays RIG_SETUP-only: rename/pivot are rig-editing actions and
                // would only clutter the canvas while drawing.
                val authoringActor = isActorStudioSession && state.scene.studioMode != StudioMode.ANIMATE_IK
                frameReference?.takeIf { toolbarVisible }?.let { reference ->
                    androidx.compose.runtime.key(reference) {
                        com.donnclab.kinetiframe.ui.actors.ActorReferenceControls(
                            reference, state.project.stage.width, state.project.stage.height,
                            Modifier.align(Alignment.TopEnd).padding(8.dp),
                            description = "Reference for frame ${state.animation.currentFrame + 1} (temporary)",
                            onReplace = { image -> frameReferences.replace(reference, image,
                                state.project.stage.width, state.project.stage.height) }
                        )
                    }
                }
                if (toolbarVisible && authoringActor && actorReference != null) {
                    com.donnclab.kinetiframe.ui.actors.ActorReferenceControls(actorReference,
                        state.project.stage.width, state.project.stage.height,
                        Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                if (authoringActor) {
                    val definition = state.actorDefinition()
                    if (toolbarVisible && definition != null && !state.actor.isPerformanceAuthoring) {
                        Box(Modifier.align(Alignment.TopStart).padding(start = 96.dp, top = 8.dp)) {
                            com.donnclab.kinetiframe.ui.actors.sketch.ActorDrawingModeButton(
                                definition.sceneGraph.drawing != null,
                                definition.sceneGraph.drawing?.enabled == true,
                                onSketch = ::openActorSketch,
                                onMode = { viewModel.handleIntent(StudioIntent.Actor.UseDrawing(it)) }
                            )
                        }
                    }
                    val selectedBone = definition?.sceneGraph?.bones?.get(state.scene.selectedBoneId)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedBone != null && definition?.sceneGraph?.drawing?.enabled == true) {
                                com.donnclab.kinetiframe.ui.actors.sketch.ActorSketchJointControls(
                                    { viewModel.handleIntent(StudioIntent.Actor.SetEditTarget(it)) },
                                    ::openActorSketch
                                ) {
                                    com.donnclab.kinetiframe.ui.actors.sketch.ActorSketchArtworkControls(selectedBone,
                                        selectedBone.id in (requireNotNull(definition.sceneGraph.drawing).overlayParts + requireNotNull(definition.sceneGraph.drawing).underlayParts), viewModel::handleIntent,
                                        { chooseActorArtwork(false); replacementArtwork = it.takeIf(String::isNotEmpty) },
                                        { editingArtwork = selectedBone.id to it }, behind = selectedBone.id in requireNotNull(definition.sceneGraph.drawing).underlayParts)
                                }
                            }
                            if (selectedBone != null && state.scene.studioMode == StudioMode.RIG_SETUP && definition?.sceneGraph?.drawing?.enabled != true) {
                                PartActionsRow(
                                    bone = selectedBone,
                                    setup = true,
                                    editTarget = state.actor.editTarget,
                                    canCaptureSketch = state.currentBitmap != null,
                                    onIntent = viewModel::handleIntent,
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    onChooseArtwork = { chooseActorArtwork(false) },
                                    onEditArtwork = { editingArtwork = selectedBone.id to it },
                                    onReplaceArtwork = { chooseActorArtwork(false); replacementArtwork = it.takeIf(String::isNotEmpty) }
                                )
                            }
                            if (selectedBone != null && state.scene.studioMode == StudioMode.RASTER_DRAWING && !state.actor.isPerformanceAuthoring) {
                                com.donnclab.kinetiframe.ui.components.rigging.ActorArtworkControls(selectedBone, viewModel::handleIntent,
                                    onAttach = { chooseActorArtwork(false); replacementArtwork = it.takeIf(String::isNotEmpty) },
                                    onEdit = { editingArtwork = selectedBone.id to it })
                            }
                            if (definition != null) {
                                PartRail(
                                    graph = definition.sceneGraph,
                                    selectedPartId = state.scene.selectedBoneId,
                                    onPartSelected = { partId -> viewModel.handleIntent(StudioIntent.Rigging.SelectBone(partId)) }
                                )
                            }
                        }
                    }
                }

                if (toolbarVisible && isActorStudioSession && state.scene.studioMode == StudioMode.ANIMATE_IK) {
                    state.actorDefinition()?.sceneGraph?.bones?.get(state.scene.selectedBoneId)
                        ?.takeIf { it.artworkVariants.isNotEmpty() }?.let { part ->
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                                com.donnclab.kinetiframe.ui.components.rigging.ActorArtworkMenu(part, viewModel::handleIntent)
                            }
                        }
                }

                if (showPartPicker) {
                    state.actorDefinition()?.let { definition ->
                        PartPickerSheet(
                            onReferenceClick = actorReference?.let { reference -> { showPartPicker = false; reference.menuOpen = true } },
                            state = state,
                            definition = definition,
                            onIntent = viewModel::handleIntent,
                            onDismiss = { showPartPicker = false }
                        )
                    }
                }

                if (showPresetLibrary) {
                    PresetLibrarySheet(
                        presets = state.actor.presets.filter {
                            it.supportsView(state.actor.authoredView) && it.missingBones(state.actorDefinition()?.sceneGraph?.bones?.keys.orEmpty()).isEmpty()
                        },
                        previewingId = state.actor.presetPreview?.recipe?.id,
                        onPreview = {
                            if (state.actor.previewPlaying) viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                            viewModel.handleIntent(StudioIntent.ActorPreset.Preview(it))
                            viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                        },
                        onApply = {
                            if (state.actor.previewPlaying) viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                            viewModel.handleIntent(StudioIntent.ActorPreset.Apply)
                            showPresetLibrary = false
                        },
                        canApply = state.actor.presetPreview != null,
                        preview = state.actor.presetPreview,
                        playing = state.actor.previewPlaying,
                        replacesKeys = !state.activeActor()?.poseKeys.isNullOrEmpty() || !state.activeActor()?.rotationCurves.isNullOrEmpty(),
                        onPlay = { viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback) },
                        onIntensity = { viewModel.handleIntent(StudioIntent.ActorPreset.SetIntensity(it)) },
                        onSpeed = {
                            if (state.actor.previewPlaying) viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                            viewModel.handleIntent(StudioIntent.ActorPreset.SetSpeed(it))
                            viewModel.handleIntent(StudioIntent.ActorMotion.Scrub(0))
                            viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                        },
                        onRemove = if (state.activeActor()?.motionPreset != null) ({
                            if (state.actor.previewPlaying) viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                            viewModel.handleIntent(StudioIntent.ActorPreset.Remove)
                            showPresetLibrary = false
                        }) else null,
                        onDismiss = {
                            if (state.actor.previewPlaying) viewModel.handleIntent(StudioIntent.ActorMotion.TogglePlayback)
                            viewModel.handleIntent(StudioIntent.ActorPreset.Cancel)
                            showPresetLibrary = false
                        }
                    )
                }

                // Keep the dock as the final overlay so it remains draggable
                // even when the user places it over the timeline or canvas.
                if (toolbarVisible && (!state.animation.isPlaying || !playbackHidesTools)) {
                    ToolDock(
                        state = state,
                        onIntent = { intent ->
                            if (com.donnclab.kinetiframe.ui.components.camera.CameraWorkspacePolicy.selectsArtwork(intent)) {
                                cameraPreview = false
                                if (state.animation.isPlaying) viewModel.handleIntent(StudioIntent.Playback.Pause)
                            }
                            viewModel.handleIntent(intent)
                        },
                        onInteraction = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (toolbarVisible && !isPuppetStudio && state.selection.active && !state.animation.isPlaying) {
                    SelectionDock(
                        state = state,
                        onIntent = viewModel::handleIntent,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Floating side-panel tray -- opened by tapping a PACK in the Library picker (see
                // onOpenPack above), stays open across repeated drags so a mouth or eye pack can be
                // used many times on the same frame or across frames.
                packTrayManifest?.takeIf { toolbarVisible }?.let { manifest ->
                    com.donnclab.kinetiframe.ui.packs.PackTray(
                        packName = manifest.displayName,
                        items = packTrayItems,
                        loading = packTrayLoading,
                        error = null,
                        onDismiss = { packTrayManifest = null; packTrayItems = emptyList() },
                        onDrag = { event ->
                            when (event) {
                                is com.donnclab.kinetiframe.ui.packs.PackDragEvent.Start -> {
                                    packDragItem = event.item
                                    packDragPosition = event.positionInWindow
                                }
                                is com.donnclab.kinetiframe.ui.packs.PackDragEvent.Move -> {
                                    packDragPosition = packDragPosition?.plus(event.delta)
                                }
                                is com.donnclab.kinetiframe.ui.packs.PackDragEvent.End -> applyPackDrop()
                                is com.donnclab.kinetiframe.ui.packs.PackDragEvent.Cancel -> {
                                    packDragItem = null
                                    packDragPosition = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // The dragged drawing's own ghost, following the finger -- rendered as the very
                // last overlay so it's never hidden behind the tray or the tool dock mid-drag.
                packDragItem?.let { item ->
                    val position = (packDragPosition ?: Offset.Zero) - canvasRootPositionInWindow
                    Image(
                        bitmap = item.thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .offset {
                                IntOffset((position.x - 32.dp.toPx()).roundToInt(), (position.y - 32.dp.toPx()).roundToInt())
                            }
                            .size(64.dp)
                            .alpha(0.85f)
                    )
                }
            }
        }
    }
}
