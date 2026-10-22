package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.data.actor.ActorArtworkStore
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.ui.EditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Landscape destination that hosts the shared canvas engine against an isolated actor document. */
@Composable
fun ActorStudioScreen(
    target: ActorStudioTarget,
    repositories: ActorStudioRepositories,
    onExit: () -> Unit,
    onPerformanceSaved: (PerformanceDocument) -> Unit = {},
) {
    // Unpacked once here so the session body below reads against plain locals; the grouping exists
    // for the call boundary (see ActorStudioTarget), not to make every use site verbose.
    val actorId = target.actorId
    val projectId = target.projectId
    val performanceId = target.performanceId
    val returnPlacementId = target.returnPlacementId
    val repository = repositories.actors
    val performances = repositories.performances
    val artwork = repositories.artwork
    // Performance editing is a distinct document context even when the actor is the same.
    val model: ActorStudioDocumentViewModel =
        viewModel(
            key = "actor-studio-$actorId-${projectId ?: "setup"}-${performanceId ?: "new"}",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ActorStudioDocumentViewModel(target, repository, performances) as T
            },
        )
    val state by model.state.collectAsStateWithLifecycle()
    val saveScope = rememberCoroutineScope()
    var writingArtwork by remember { mutableStateOf(false) }
    val references =
        remember(actorId) {
            mutableMapOf<com.donnclab.kinetiframe.engine.rigging.model.ActorView, ActorReferenceSession>()
        }
    var error by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var saveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var saveAndExitPending by remember { mutableStateOf(false) }
    var showPerformanceActions by remember { mutableStateOf(false) }
    var duplicatePerformanceName by remember { mutableStateOf<String?>(null) }
    var pendingPerformance by remember {
        mutableStateOf<Pair<com.donnclab.kinetiframe.engine.model.PuppetInstance, Long>?>(null)
    }
    val manifest = state.manifest
    val revision = state.revision
    val stateError = state.error
    LaunchedEffect(model) {
        model.effect.collectLatest { effect ->
            when (effect) {
                ActorStudioDocumentEffect.Exit -> onExit()

                ActorStudioDocumentEffect.ConfirmDiscard -> showDiscardConfirmation = true

                is ActorStudioDocumentEffect.PerformanceSaved -> {
                    onPerformanceSaved(effect.document)
                    onExit()
                }

                is ActorStudioDocumentEffect.ShowError -> {
                    saveAndExitPending = false
                    error = effect.message
                }
            }
        }
    }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        stateError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FilledTonalButton(onClick = { model.handleIntent(ActorStudioDocumentIntent.Reload) }) { Text(stateError) }
        }

        manifest != null && revision != null -> RestoredActorEditor(
            actorId = actorId,
            document = ActorStudioDocumentSnapshot(manifest, revision, state.performance),
            artwork = artwork,
            performanceAuthoring = projectId != null,
            reference = references.getOrPut(revision.view) { ActorReferenceSession() },
            actorViews = {
                ActorViewMenu(
                    revision.view,
                    manifest.authoredViews().keys,
                    state.performance != null,
                    canCreate = projectId == null,
                    onSelect = { model.handleIntent(ActorStudioDocumentIntent.SelectView(it)) },
                    onCreate = { model.handleIntent(ActorStudioDocumentIntent.CreateView(it)) },
                )
            },
            callbacks = ActorStudioEditorCallbacks(
                onSaveActorRevision = save@{ definition, runtime ->
                    if (writingArtwork) return@save
                    writingArtwork = true
                    val exitAfterSaving = saveAndExitPending
                    saveAndExitPending = false
                    val revisionId = "revision-${java.util.UUID.randomUUID()}"
                    // PNG compression can be expensive. Keep it off the Compose event path before
                    // atomically committing the revision document that references the artwork.
                    saveScope.launch {
                        val paths = runCatching {
                            withContext(Dispatchers.IO) {
                                artwork.save(manifest.actorId, revisionId, definition.sceneGraph, runtime)
                            }
                        }.getOrElse { throwable ->
                            error = throwable.message ?: "Could not save actor artwork"
                            writingArtwork = false
                            return@launch
                        }
                        model.handleIntent(
                            ActorStudioDocumentIntent.SaveGraph(
                                definition.sceneGraph,
                                revisionId,
                                paths,
                                exitAfterSaving,
                            ),
                        )
                        writingArtwork = false
                    }
                },
                onSavePerformance = { instance, durationMicros -> pendingPerformance = instance to durationMicros },
                onPerformanceActions = state.performance?.let { { showPerformanceActions = true } },
                onExit = {
                    if (!writingArtwork &&
                        !saveAndExitPending
                    ) {
                        model.handleIntent(ActorStudioDocumentIntent.RequestExit)
                    }
                },
                onDirtyChanged = { model.handleIntent(ActorStudioDocumentIntent.SetDirty(it)) },
                onSaveActionAvailable = { saveAction = it },
                onSavePreparationFailed = { saveAndExitPending = false },
            ),
        )
    }
    if (writingArtwork || state.isSaving ||
        (saveAndExitPending && pendingPerformance == null)
    ) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            CircularProgressIndicator()
        }
    }
    pendingPerformance?.let { (instance, durationMicros) ->
        ActorPerformanceNameDialog(
            initialName =
            state.performance?.displayName ?: "New performance",
            onDismiss = {
                pendingPerformance = null
                saveAndExitPending =
                    false
            },
            onSave = { name ->
                model.handleIntent(
                    ActorStudioDocumentIntent.SavePerformance(
                        name,
                        instance.poseKeys,
                        instance.motionPreset,
                        durationMicros,
                        instance.rotationCurves,
                        instance.poseInterpolation,
                        instance.artworkKeys,
                        instance.rootMotionCurves,
                    ),
                )
                pendingPerformance = null
            },
        )
    }
    duplicatePerformanceName?.let { name ->
        ActorPerformanceNameDialog(
            initialName = name,
            title = "Duplicate performance",
            confirmLabel = "Duplicate",
            onDismiss = { duplicatePerformanceName = null },
            onSave = { copiedName ->
                model.handleIntent(ActorStudioDocumentIntent.DuplicatePerformance(copiedName))
                duplicatePerformanceName = null
            },
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Actor Studio") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
    if (showDiscardConfirmation) {
        com.donnclab.kinetiframe.ui.components.dialogs.SaveBeforeExitDialog(
            onSave = {
                showDiscardConfirmation = false
                saveAndExitPending = true
                saveAction?.invoke()
            },
            onDiscard = {
                showDiscardConfirmation = false
                saveAndExitPending = false
                model.handleIntent(ActorStudioDocumentIntent.DiscardAndExit)
            },
            onCancel = {
                showDiscardConfirmation = false
                saveAndExitPending = false
            },
            canSave = saveAction != null && !writingArtwork && !state.isSaving,
        )
    }
    if (showPerformanceActions) {
        AlertDialog(
            onDismissRequest = { showPerformanceActions = false },
            title = { Text("Performance actions") },
            text = {
                Text(
                    if (returnPlacementId == null) {
                        "Duplicate this reusable gesture or archive it from this project."
                    } else {
                        "This performance is currently used by the placement you are editing. Duplicate it to make a variant; archive is unavailable until the placement is removed."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPerformanceActions = false
                    model.handleIntent(ActorStudioDocumentIntent.ArchivePerformance)
                }, enabled = returnPlacementId == null) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPerformanceActions = false
                    duplicatePerformanceName = "${state.performance?.displayName ?: "Performance"} copy"
                }) { Text("Duplicate") }
            },
        )
    }
}

/** Decodes artwork before the canvas starts, retaining the texture IDs encoded in the revision. */
@Composable
private fun RestoredActorEditor(
    actorId: String,
    document: ActorStudioDocumentSnapshot,
    artwork: ActorArtworkStore,
    performanceAuthoring: Boolean,
    reference: ActorReferenceSession,
    actorViews: @Composable () -> Unit,
    callbacks: ActorStudioEditorCallbacks,
) {
    val manifest = document.manifest
    val revision = document.revision
    val performance = document.performance
    val runtime = remember(manifest.actorId, revision.revisionId) { RigAssetManager() }
    // A NavEntry outlives view/revision switches. Give only the canvas a disposable store,
    // otherwise every visited view retains its TimelineViewModel and decoded textures until exit.
    val canvasOwner = remember(manifest.actorId, revision.revisionId) {
        object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore = androidx.lifecycle.ViewModelStore()
        }
    }
    DisposableEffect(canvasOwner) { onDispose { canvasOwner.viewModelStore.clear() } }
    var restored by remember(manifest.actorId, revision.revisionId) { mutableStateOf(false) }
    var restoreError by remember(manifest.actorId, revision.revisionId) { mutableStateOf<String?>(null) }
    LaunchedEffect(manifest.actorId, revision.revisionId) {
        try {
            val missing = withContext(Dispatchers.IO) {
                artwork.restore(manifest.actorId, revision.artworkAssetPaths, revision.sceneGraph, runtime)
            }
            check(missing.isEmpty()) { "Missing actor artwork: ${missing.joinToString()}" }
            restored = true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            runtime.clear() // No canvas has been shown yet, so no render/history owns these textures.
            restoreError = error.message ?: "Could not restore actor artwork"
        }
    }
    restoreError?.let { message ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = callbacks.onExit) { Text(message) }
        }
        return
    }
    if (!restored) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    // A saved revision is immutable. Recreate only this isolated adapter when its ID changes so
    // the document just saved becomes the clean baseline for future dirty-state checks.
    CompositionLocalProvider(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides canvasOwner) {
        EditorScreen(
            initialEditorState = actorStudioInitialState(
                manifest = manifest,
                revision = revision,
                performance = performance,
                performanceAuthoring = performanceAuthoring,
            ),
            // Scopes this session's scratch frame cache to this exact actor + revision, so
            // returning to the same actor keeps its own sketch cache while a different actor
            // (or even the same actor's next revision) never reads back another session's
            // leftover frame bitmaps -- see EditorScreen's cacheNamespace doc.
            cacheNamespace = "actor-$actorId-${revision.revisionId}",
            actorReference = if (performanceAuthoring) null else reference,
            onSaveActorRevision = callbacks.onSaveActorRevision,
            onSavePerformance = callbacks.onSavePerformance,
            onPerformanceActions = callbacks.onPerformanceActions,
            actorViews = actorViews,
            onExitActorStudio = callbacks.onExit,
            runtimeRigAssetManager = runtime,
            onActorStudioDirtyChanged = callbacks.onDirtyChanged,
            onActorSaveActionAvailable = callbacks.onSaveActionAvailable,
            onActorSavePreparationFailed = callbacks.onSavePreparationFailed,
        )
    }
}
