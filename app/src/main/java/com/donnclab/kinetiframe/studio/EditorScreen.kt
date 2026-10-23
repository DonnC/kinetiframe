package com.donnclab.kinetiframe.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donnclab.kinetiframe.actors.data.ActorPresetRepository
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.actors.ui.ActorReferenceSession
import com.donnclab.kinetiframe.library.ui.packs.PackCreatorConfig
import com.donnclab.kinetiframe.project.data.ProjectFrameDraft
import com.donnclab.kinetiframe.project.data.ProjectSaveDocument
import com.donnclab.kinetiframe.project.model.ActorPlacementReference
import com.donnclab.kinetiframe.project.model.PuppetDefinition
import com.donnclab.kinetiframe.project.model.PuppetInstance
import com.donnclab.kinetiframe.project.ui.ProjectDraftSession
import com.donnclab.kinetiframe.project.ui.ProjectDraftSessionIntent
import com.donnclab.kinetiframe.studio.editor.EditorFolders
import com.donnclab.kinetiframe.studio.editor.EditorHost
import com.donnclab.kinetiframe.studio.editor.EditorWorkspace
import com.donnclab.kinetiframe.studio.editor.rememberEditorRepositories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The drawing studio for a gallery project, a Pack Creator draft or Actor Studio's canvas. It
 * loads the document, gives it a [StudioViewModel] of its own and shows the editor around it.
 */
@Composable
fun EditorScreen(
    actorReference: ActorReferenceSession? = null,
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
    onSaveActorRevision: ((PuppetDefinition, RigAssetManager) -> Unit)? = null,
    onSavePerformance: ((PuppetInstance, Long) -> Unit)? = null,
    onPerformanceActions: (() -> Unit)? = null,
    actorViews: (@Composable () -> Unit)? = null,
    onExitActorStudio: (() -> Unit)? = null,
    onCreatePerformance: ((String, String) -> Unit)? = null,
    onEditPerformance: ((ActorPlacementReference, String) -> Unit)? = null,
    runtimeRigAssetManager: RigAssetManager? = null,
    returnedPerformance: PerformanceDocument? = null,
    onReturnedPerformanceConsumed: () -> Unit = {},
    /** Actor Studio observes only durable rig/gesture edits, never transient selection or preview state. */
    onActorStudioDirtyChanged: ((Boolean) -> Unit)? = null,
    onActorSaveActionAvailable: (((() -> Unit)?) -> Unit)? = null,
    onActorSavePreparationFailed: (() -> Unit)? = null,
    /** Non-null only when this screen is hosting the Pack Creator micro-editor
     *  ([com.donnclab.kinetiframe.library.ui.packs.PackCreatorRoute]) rather than an ordinary project. */
    packCreatorConfig: PackCreatorConfig? = null,
    onSettingsClick: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
) {
    val host = EditorHost(
        projectId = projectId,
        initialEditorState = initialEditorState,
        packCreatorConfig = packCreatorConfig,
        actorReference = actorReference,
        actorViews = actorViews,
        onSaveActorRevision = onSaveActorRevision,
        onSavePerformance = onSavePerformance,
        onPerformanceActions = onPerformanceActions,
        onExitActorStudio = onExitActorStudio,
        onCreatePerformance = onCreatePerformance,
        onEditPerformance = onEditPerformance,
        returnedPerformance = returnedPerformance,
        onReturnedPerformanceConsumed = onReturnedPerformanceConsumed,
        onActorStudioDirtyChanged = onActorStudioDirtyChanged,
        onActorSaveActionAvailable = onActorSaveActionAvailable,
        onActorSavePreparationFailed = onActorSavePreparationFailed,
        onHomeClick = onHomeClick,
        onBackClick = onBackClick,
    )
    val context = LocalContext.current
    val repositories = rememberEditorRepositories(context)
    val actorStore = repositories.actorStore

    // Saved frames/assets remain project-owned. Raster edits use a private working cache;
    // evictions are not implicit saves, so Discard preserves the committed project pixels.
    // Pack Creator already owns a disposable project and keeps its existing save adapter.
    // A caller with no project ID -- Actor Studio's embedded canvas, or any other scratch session
    // -- gets its own namespaced subdirectory instead of ever touching a directory another
    // session could also land in: [cacheNamespace] when the caller supplies one (Actor Studio's
    // actor+revision identity), or a random one generated once per composition as a last resort
    // so an un-namespaced caller still can't collide with anyone else.
    val fallbackCacheNamespace = remember { "session-${UUID.randomUUID()}" }
    val projectDraft = if (projectId != null && packCreatorConfig == null) {
        viewModel<ProjectDraftSession>(key = "project-draft-$projectId")
    } else {
        null
    }
    val editorCacheDir = remember(projectId, cacheNamespace) {
        projectId?.let {
            if (packCreatorConfig != null) {
                File(actorStore.projectDirectory(it), "frames")
            } else {
                File(context.cacheDir, "editor-scratch/${requireNotNull(projectDraft).state.value.id}/frames")
            }
        }
            ?: File(context.cacheDir, "editor-scratch/${cacheNamespace ?: fallbackCacheNamespace}")
    }
    val editorFilesDir = remember(projectId, cacheNamespace) {
        projectId?.let { actorStore.projectDirectory(it) }
            ?: File(context.filesDir, "editor-scratch/${cacheNamespace ?: fallbackCacheNamespace}").apply { mkdirs() }
    }

    var projectLoading by remember(projectId) { mutableStateOf(projectId != null && initialEditorState == null) }
    var projectLoadError by remember(projectId) { mutableStateOf<String?>(null) }
    var loadedProjectDocument by remember(projectId) { mutableStateOf<ProjectSaveDocument?>(null) }

    LaunchedEffect(projectId) {
        if (projectId == null || initialEditorState != null) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            val loaded = repositories.projects.load(projectId)
            if (loaded is ActorStoreResult.Value && projectDraft?.state?.value?.ready == false) {
                try {
                    ProjectFrameDraft.open(File(actorStore.projectDirectory(projectId), "frames"), editorCacheDir)
                } catch (failure: Exception) {
                    return@withContext ActorStoreResult.Failure(
                        failure.message ?: "Could not open frame draft",
                        failure,
                    )
                }
            }
            loaded
        }
        when (result) {
            is ActorStoreResult.Value -> {
                loadedProjectDocument = result.value.second
                projectDraft?.handleIntent(ProjectDraftSessionIntent.Prepared)
            }

            is ActorStoreResult.Failure -> projectLoadError = result.message
        }
        projectLoading = false
    }

    if (projectLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (projectLoadError != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not open this project: $projectLoadError")
        }
        return
    }

    // Only the structural, content-bearing part of a saved project round-trips here -- transient
    // view state (zoom/pan, tool selection, onion-skin/grid settings) resets to defaults on load.
    val effectiveInitialState = initialEditorState ?: loadedProjectDocument?.let { doc ->
        val firstFrameLayers = doc.frameLayers[0] ?: doc.project.layers
        StudioState(
            project = doc.project,
            scene = SceneState(nodes = doc.project.nodes),
            animation = AnimationState(totalFrames = doc.totalFrames, fps = doc.project.fps),
            layers = LayerState(
                layers = firstFrameLayers,
                frameLayers = doc.frameLayers,
                linkedCels = doc.linkedCels,
                activeLayerIndex = doc.activeLayerIndex.coerceIn(0, (firstFrameLayers.size - 1).coerceAtLeast(0)),
            ),
        )
    }

    // `viewModel()` without a key resolves to one slot per ViewModel class, so a second editor
    // would silently reuse the first one's canvas: the factory isn't called on a cache hit.
    // Navigation scopes stores per entry; this key also separates documents inside one entry,
    // such as Actor Studio's own canvas or a revision switch. It is the same identity that scopes
    // the frame cache above, so the ViewModel and its pixels always agree.
    val documentKey = remember(projectId, cacheNamespace) {
        "editor-${projectId ?: cacheNamespace ?: fallbackCacheNamespace}"
    }
    val viewModel: StudioViewModel = viewModel(
        key = documentKey,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StudioViewModel(
                editorCacheDir,
                loadActorPresets = ActorPresetRepository(context.assets)::load,
                initialEditorState = effectiveInitialState?.copy(packTags = packCreatorConfig?.tags.orEmpty()),
                runtimeRigAssetManager = runtimeRigAssetManager,
                filesDir = editorFilesDir,
                diagnosticsDir = context.filesDir,
            ) as T
        },
    )
    EditorWorkspace(viewModel, host, repositories, EditorFolders(cache = editorCacheDir, files = editorFilesDir))
}
