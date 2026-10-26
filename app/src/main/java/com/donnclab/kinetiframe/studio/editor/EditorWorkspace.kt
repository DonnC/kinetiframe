// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.actors.render.ActorPlacementPreparation
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.library.domain.LibraryPick
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.ui.actorstudio.hasActorStudioChanges
import com.donnclab.kinetiframe.studio.ui.actorstudio.sketch.ActorSketchEditor
import com.donnclab.kinetiframe.studio.ui.timeline.LayerPanel
import com.donnclab.kinetiframe.studio.ui.timeline.durationFramesFor
import com.donnclab.kinetiframe.ui.appViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Where the editor keeps its files. */
internal data class EditorFolders(
    /** The working frame cache. For a gallery project it is a draft that only a save publishes. */
    val cache: File,
    /** Durable storage for the project's own assets. */
    val files: File,
)

/** The editor around an open document: its bars, canvas, dialogs, saves and the effects that keep them in step. */
@Composable
internal fun EditorWorkspace(
    viewModel: StudioViewModel,
    host: EditorHost,
    repositories: EditorRepositories,
    folders: EditorFolders,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = rememberEditorUiState()
    val messages = ui.messages
    val panels = ui.panels
    val scope = rememberCoroutineScope()
    val media = remember(context, folders.files) { EditorMediaImporter(context, folders.files) }
    val storage: EditorStorageViewModel = appViewModel()
    val placementPreparation = remember(repositories) {
        ActorPlacementPreparation(repositories.actors, repositories.performances, repositories.actorStore)
    }

    fun placeActor(reference: ActorPlacementReference) {
        val project = state.project
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { placementPreparation.prepare(reference, project) } }
                .onSuccess {
                    if (viewModel.state.value.project.id == project.id) {
                        viewModel.handleIntent(StudioIntent.ActorPlacement.Add(it))
                    }
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    messages.placementError = it.message ?: "Could not place performance"
                }
        }
    }
    messages.placementError?.let { MessageDialog("Performance placement", it) { messages.placementError = null } }
    // Repair only legacy identity placements entirely outside the stage; never reposition an
    // explicitly transformed character or change an already visible saved composition.
    LaunchedEffect(state.project.id) {
        val project = state.project
        project.actorPlacements.values.forEach { reference ->
            runCatching {
                withContext(Dispatchers.IO) { placementPreparation.prepare(reference, project, repairOnly = true) }
            }.onSuccess { repaired ->
                if (repaired.transform != reference.transform) {
                    viewModel.handleIntent(
                        StudioIntent.ActorPlacement.UpdateTransform(reference.placementId, repaired.transform),
                    )
                }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.handleIntent(StudioIntent.ActorMotion.CancelTake) }
    DisposableEffect(viewModel) { onDispose { viewModel.handleIntent(StudioIntent.ActorMotion.CancelTake) } }
    // Actor Studio observes only durable rig and gesture edits, never selection or preview state.
    val actorStudioDirty = hasActorStudioChanges(host.initialEditorState, state)
    LaunchedEffect(actorStudioDirty) { host.onActorStudioDirtyChanged?.invoke(actorStudioDirty) }
    LaunchedEffect(host.returnedPerformance?.performanceId) {
        val performance = host.returnedPerformance ?: return@LaunchedEffect
        // Never consume a return result against a newly opened/different project. The result is
        // project-scoped and must remain available until the originating editor is restored.
        if (performance.projectId != state.project.id) return@LaunchedEffect
        if (performance.performanceId !in state.project.actorPlacements.values.map { it.performanceId }) {
            placeActor(state.placementFor(performance))
        }
        host.onReturnedPerformanceConsumed()
    }
    val frameReferences = viewModel.frameReferences
    DisposableEffect(frameReferences) { onDispose { frameReferences.clear() } }
    LaunchedEffect(state.selection.selectedContent?.videos) {
        if (!state.selection.selectedContent?.videos.isNullOrEmpty()) panels.tracksWorkspaceVisible = false
    }
    // Keep engine errors observed while the source workspace replaces the main canvas.
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is StudioEffect.ShowError -> messages.timelineError = effect.message
                is StudioEffect.ShowTextAddDialog -> panels.textAddDialog = effect.x to effect.y
                is StudioEffect.ShowTextEditDialog -> panels.textEditDialog = effect.id to effect.initialText
            }
        }
    }

    val density = LocalDensity.current.density
    val projectSave = host.projectId?.let { projectId ->
        val draftFrames = if (host.isPackCreator) null else folders.cache
        ProjectSaveTarget(projectId, storage, draftFrames, density)
    }
    val saves = EditorSaveActions(viewModel, messages, ui.exit, scope, projectSave)
    val actorSaves = ActorStudioSaves(viewModel, messages, host)
    // Register one stable action with Actor Studio; it reads the current canvas when it runs.
    val latestActorSaves by rememberUpdatedState(actorSaves)
    DisposableEffect(Unit) {
        host.onActorSaveActionAvailable?.invoke { latestActorSaves.saveOnExit() }
        onDispose { host.onActorSaveActionAvailable?.invoke(null) }
    }

    fun requestExit(destination: () -> Unit) {
        viewModel.handleIntent(StudioIntent.Playback.Pause)
        if (host.isActorStudioSession) actorSaves.exit() else saves.confirmExit(destination)
    }
    EditorExitDialogs(ui.exit, saves, host.isPackCreator)
    val back = host.onBackClick ?: host.onHomeClick ?: host.onExitActorStudio
    BackHandler(enabled = back != null) { requestExit(requireNotNull(back)) }

    val pickers = rememberEditorMediaPickers(viewModel, media, ui, scope)
    val artwork = ui.artwork
    val openSketch = { artwork.openSketch(viewModel, messages) }
    // A whole-surface source must never silently receive selected-part raster strokes.
    val drawingEnabled = state.actorDefinition()?.sceneGraph?.drawing?.enabled == true
    LaunchedEffect(drawingEnabled, state.scene.studioMode) {
        if (host.isActorStudioSession && !state.actor.isPerformanceAuthoring && drawingEnabled &&
            state.scene.studioMode == StudioMode.RASTER_DRAWING
        ) {
            openSketch()
        }
    }
    if (artwork.sketchOpen) {
        state.actorDefinition()?.let { definition ->
            ActorSketchEditor(
                definition,
                state.project.stage.width,
                state.project.stage.height,
                viewModel.rigAssetManager,
                definition.sceneGraph.drawing?.inkAssetId,
                viewModel::handleIntent,
                { artwork.sketchOpen = false },
            )
            TimelineErrorDialog(messages)
            return
        }
    }
    ActorArtworkFlows(artwork, state, viewModel)
    LaunchedEffect(ui.tray.manifest) {
        val manifest = ui.tray.manifest ?: return@LaunchedEffect
        ui.tray.load(manifest, storage, state.project.id)
    }

    /** Places a picked library item where its kind belongs. */
    fun applyLibraryPick(manifest: LibraryItemManifest) {
        panels.showLibraryPicker = false
        scope.launch {
            storage.pickFromLibrary(manifest, state.project.id, System.currentTimeMillis(), media.audioDirectory).fold(
                onSuccess = { pick ->
                    val current = viewModel.state.value
                    when (pick) {
                        is LibraryPick.Audio -> viewModel.handleIntent(
                            current.addAudioClip(
                                "audio-${UUID.randomUUID()}",
                                pick.path,
                                pick.durationMicros,
                                pick.label,
                            ),
                        )

                        is LibraryPick.Background -> viewModel.handleIntent(
                            StudioIntent.Background.SetImage(pick.bitmap),
                        )

                        is LibraryPick.Stamp -> viewModel.handleIntent(StudioIntent.Selection.StampBitmap(pick.bitmap))

                        is LibraryPick.Actor -> messages.libraryMessage = "Added \"${pick.displayName}\" to your actors"

                        is LibraryPick.Performance -> {
                            val performance = pick.document
                            placeActor(
                                ActorPlacementReference(
                                    "placement-${UUID.randomUUID()}",
                                    performance.performanceId,
                                    performance.actorId,
                                    performance.actorRevisionId,
                                    performance.displayName,
                                    current.animation.currentFrame,
                                    durationFramesFor(performance, current.project.fps),
                                    zIndex = current.animation.currentFrame,
                                ),
                            )
                        }
                    }
                },
                onFailure = { messages.libraryError = it.message ?: "Could not add that item" },
            )
        }
    }

    val commands = EditorCommands(
        onIntent = viewModel::handleIntent,
        requestExit = ::requestExit,
        saves = saves,
        actorSaves = actorSaves,
        placeActor = ::placeActor,
        openSketch = openSketch,
        pickers = pickers,
        exports = EditorLibraryExports(storage, viewModel, ui, scope),
    )
    EditorDialogs(viewModel, state, ui, repositories, ::applyLibraryPick)
    VoiceOverFlow(viewModel, media, ui, scope)
    val drawerState = ui.drawerState
    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }
    host.packCreatorConfig?.let { PackCreatorDialogs(it, state, ui, viewModel::handleIntent) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                LayerPanel(
                    state = state,
                    onIntent = viewModel::handleIntent,
                    onLayerClick = { scope.launch { drawerState.close() } },
                )
            }
        },
    ) {
        Scaffold(
            topBar = { EditorTopBarSlot(state, host, ui, commands) },
            bottomBar = { EditorBottomBarSlot(viewModel, state, host, ui, repositories, commands) },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Pack drags are measured against this Box. The canvas and the pack tray
                    // both fill it, so a drag's window position minus this one is canvas-local.
                    .onGloballyPositioned { coordinates ->
                        ui.tray.canvasRootPositionInWindow = coordinates.positionInWindow()
                    },
            ) {
                EditorCanvasStack(viewModel, state, host, ui, repositories, commands)
            }
        }
    }
}
