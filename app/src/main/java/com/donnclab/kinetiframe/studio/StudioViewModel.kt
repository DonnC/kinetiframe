package com.donnclab.kinetiframe.studio

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.studio.ui.reference.FrameReferenceSession
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * The studio's MVI boundary. It owns [StudioState] and [StudioEffect], applies the rules that cut
 * across every intent, and routes each intent group to [StudioCanvasIntents], [StudioSceneIntents]
 * or a manager in [StudioSession].
 */
class StudioViewModel(
    cacheDir: File,
    private val engineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val shouldInitialize: Boolean = true,
    private val loadActorPresets: suspend () -> List<com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset> = {
        emptyList()
    },
    initialEditorState: StudioState? = null,
    runtimeRigAssetManager: RigAssetManager? = null,
    /** App-private, non-cache storage root for durable project-level assets such as the
     *  persistent background image. The app passes Context.filesDir, because the OS may clear a
     *  cache directory at any time. The [cacheDir] default exists for tests that don't use
     *  backgrounds. */
    filesDir: File = cacheDir,
    /** Separate app-private diagnostics from document storage: reports must not enter exports. */
    diagnosticsDir: File = filesDir,
) : BaseViewModel<StudioState, StudioIntent, StudioEffect>(
    initialEditorState ?: StudioState(
        project = com.donnclab.kinetiframe.core.document.ProjectDocument(
            layers = listOf(Layer(name = "Main Layer", id = "main")),
        ),
        layers = LayerState(
            layers = listOf(Layer(name = "Main Layer", id = "main")),
            frameLayers = mapOf(0 to listOf(Layer(name = "Main Layer", id = "main"))),
        ),
    ),
) {

    private val store = StudioStore(viewModelScope, { currentState }, { updateState(it) }, { sendEffect(it) })
    private val session = StudioSession(
        store,
        StudioDirectories(cacheDir, filesDir, diagnosticsDir),
        engineDispatcher,
        shouldInitialize,
        loadActorPresets,
        runtimeRigAssetManager,
    )
    private val canvasIntents = StudioCanvasIntents(store, session)
    private val sceneIntents = StudioSceneIntents(store, session)

    val cacheManager = session.cacheManager
    val frameReferences = FrameReferenceSession().also { session.frameStructureListener = it }

    /** Supplied by Actor Studio when reopening a revision with persisted artwork. */
    val rigAssetManager = session.rigAssetManager

    /** Decoded bitmap for the project's persistent background image, if one is set. */
    val projectAssetManager = session.projectAssetManager
    val audioPlaybackManager = session.audioPlaybackManager

    /** EditorScreen and CanvasArea read the current video reference frame from this directly. */
    val videoFrameProvider = session.videoFrameProvider

    fun launchSave(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = session.launchSave(block)

    /** Commit floating selection and await accepted raster/frame edits before publishing a save. */
    suspend fun prepareProjectSave(nodeDensity: Float = currentState.project.nodeDensity) =
        session.prepareProjectSave(nodeDensity)

    override fun handleIntent(intent: StudioIntent) {
        if (intent.invalidatesSceneEffects()) session.sceneEffects.clear()
        // Do not carry floating pixels or detached text into another frame/layer/history session.
        if (currentState.selection.selectedContent != null && intent.leavesFloatingSelection()) {
            viewModelScope.launch {
                session.selectionManager.commitSelection().join()
                handleIntent(intent)
            }
            return
        }
        when (intent) {
            is StudioIntent.Camera -> sceneIntents.handleCamera(intent)

            is StudioIntent.PackItem -> sceneIntents.handlePackItem(intent)

            is StudioIntent.ActorPlacement -> session.actorPlacementManager.handle(intent)

            is StudioIntent.Tracks -> when (intent) {
                is StudioIntent.Tracks.ToggleGroup -> updateState { it.toggleTrackGroup(intent.group) }
            }

            is StudioIntent.ActorPreset -> session.actorAuthoring.handlePreset(intent)

            is StudioIntent.ActorMotion -> session.actorAuthoring.handleMotion(intent)

            is StudioIntent.Actor -> session.actorAuthoring.handleActor(intent)

            is StudioIntent.Playback -> canvasIntents.handlePlaybackIntent(intent)

            is StudioIntent.Draw -> canvasIntents.handleDrawIntent(intent)

            is StudioIntent.Layer -> canvasIntents.handleLayerIntent(intent)

            is StudioIntent.Frame -> canvasIntents.handleFrameIntent(intent)

            is StudioIntent.View -> session.viewManager.handle(intent)

            is StudioIntent.OnionSkin -> updateState { it.reduce(intent) }

            is StudioIntent.Rigging -> canvasIntents.handleRiggingIntent(intent)

            is StudioIntent.Text -> canvasIntents.handleTextIntent(intent)

            is StudioIntent.Selection -> canvasIntents.handleSelectionIntent(intent)

            is StudioIntent.Fill -> canvasIntents.handleFillIntent(intent)

            is StudioIntent.Construction -> canvasIntents.handleConstructionShapeIntent(intent)

            is StudioIntent.Background -> session.backgroundManager.handle(intent)

            is StudioIntent.Audio -> sceneIntents.handleAudio(intent)

            is StudioIntent.Video -> sceneIntents.handleVideo(intent)
        }
    }

    override fun onCleared() {
        session.release()
    }
}

/**
 * Audio and video edits invalidate scene-effect undo history, except the fades, crossfades, voice
 * styles, splits and framing that scene effects record themselves.
 */
private fun StudioIntent.invalidatesSceneEffects(): Boolean = when (this) {
    is StudioIntent.Audio ->
        this !is StudioIntent.Audio.SetFades && this !is StudioIntent.Audio.Crossfade &&
            this !is StudioIntent.Audio.VoiceStyle

    is StudioIntent.Video -> this !is StudioIntent.Video.Split && this !is StudioIntent.Video.Framing

    else -> false
}

/** Intents that move to another frame, layer or history session, so a floating selection is committed first. */
private fun StudioIntent.leavesFloatingSelection(): Boolean =
    this is StudioIntent.Frame || this is StudioIntent.PackItem.Add || this is StudioIntent.Layer ||
        this is StudioIntent.Playback || this is StudioIntent.Draw || this is StudioIntent.ActorPlacement ||
        this is StudioIntent.Text || this is StudioIntent.Camera || this is StudioIntent.Video ||
        this is StudioIntent.Audio
