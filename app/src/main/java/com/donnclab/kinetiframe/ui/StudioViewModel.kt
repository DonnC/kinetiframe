package com.donnclab.kinetiframe.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.engine.AnimationEngine
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.layer.LayerCompositor
import com.donnclab.kinetiframe.engine.model.DrawCommand
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.engine.rigging.texture.RigAssetManager
import com.donnclab.kinetiframe.models.*
import com.donnclab.kinetiframe.domain.manager.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * ViewModel for managing the animation timeline state and processing user intents.
 */
class StudioViewModel(
    cacheDir: File,
    private val engineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val shouldInitialize: Boolean = true,
    private val loadActorPresets: suspend () -> List<com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset> = { emptyList() },
    initialEditorState: StudioState? = null,
    runtimeRigAssetManager: RigAssetManager? = null,
    /** App-private, non-cache storage root for durable project-level assets such as the
     *  persistent background image. The app passes Context.filesDir, because the OS may clear a
     *  cache directory at any time. The [cacheDir] default exists for tests that don't use
     *  backgrounds. */
    filesDir: File = cacheDir,
    /** Separate app-private diagnostics from document storage: reports must not enter exports. */
    diagnosticsDir: File = filesDir
) : BaseViewModel<StudioState, StudioIntent, StudioEffect>(
    initialEditorState ?: StudioState(
        project = com.donnclab.kinetiframe.engine.model.ProjectDocument(
            layers = listOf(Layer(name = "Main Layer", id = "main"))
        ),
        layers = LayerState(
            layers = listOf(Layer(name = "Main Layer", id = "main")),
            frameLayers = mapOf(0 to listOf(Layer(name = "Main Layer", id = "main")))
        )
    )
) {

    private val engine = AnimationEngine(viewModelScope, engineDispatcher)
    val cacheManager = FrameCacheManager(cacheDir, viewModelScope).apply {
        restoreExposures(currentState.layers.linkedCels, currentState.animation.totalFrames)
    }
    val frameReferences = com.donnclab.kinetiframe.ui.reference.FrameReferenceSession()
    /** Supplied by Actor Studio when reopening a revision with persisted artwork. */
    val rigAssetManager = runtimeRigAssetManager ?: RigAssetManager()
    /** Decoded bitmap for the project's persistent background image, if one is set -- see
     *  [BackgroundManager]'s doc comment for why this is a separate, simpler manager than
     *  [rigAssetManager] rather than sharing it. */
    val projectAssetManager = com.donnclab.kinetiframe.engine.asset.ProjectAssetManager()
    private val bakeMutex = Mutex()
    private val frameOperationMutex = Mutex()
    private val pendingOperations = PendingOperations()
    private val saveMutex = Mutex()
    fun launchSave(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = viewModelScope.launch {
        val owner = this
        saveMutex.withLock { block(owner) }
    }

    /** Commit floating selection and await accepted raster/frame edits before publishing a save. */
    suspend fun prepareProjectSave(nodeDensity: Float = currentState.project.nodeDensity): com.donnclab.kinetiframe.data.project.ProjectSaveDocument {
        pendingOperations.awaitIdle()
        selectionManager.commitSelection().join()
        pendingOperations.awaitIdle()
        return frameOperationMutex.withLock {
            // Accepted drawing/selection jobs have already baked their pixels. Rebuilding
            // an untouched layer here creates blank artwork and can rebind an actor skin.
            val state = currentState
            val stacks = state.layers.frameLayers + (state.animation.currentFrame to state.layers.layers)
            val document = com.donnclab.kinetiframe.data.project.ProjectSaveDocument(
                project = state.project.copy(layers = stacks[0] ?: state.layers.layers,
                    nodes = state.scene.nodes.mapValues { it.value.copyWithSelection(false) }, nodeDensity = nodeDensity),
                frameLayers = stacks, totalFrames = state.animation.totalFrames,
                activeLayerIndex = state.layers.activeLayerIndex,
                linkedCels = cacheManager.exposures().filterKeys { id -> state.layers.layers.any { it.id == id } }
            )
            cacheManager.persistAllToDisk(stacks.values.flatten().map { it.id }.distinct(), document.totalFrames)
            document
        }
    }

    private val bitmapPool = java.util.concurrent.ConcurrentLinkedQueue<Bitmap>()

    private fun acquireBitmap(width: Int, height: Int): Bitmap {
        val pooled = bitmapPool.poll()
        return if (pooled != null && pooled.width == width && pooled.height == height && !pooled.isRecycled) {
            pooled.eraseColor(android.graphics.Color.TRANSPARENT)
            pooled
        } else {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    private fun releaseBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmapPool.offer(bitmap)
        }
    }

    private var history = mutableListOf<HistoryItem>()
    private var redoHistory = mutableListOf<HistoryItem>()
    private var baseBitmap: Bitmap? = null

    // Domain Managers
    private val drawingManager = DrawingManager(
        pendingOperations = pendingOperations,
        scope = viewModelScope,
        dispatcher = engineDispatcher,
        cacheManager = cacheManager,
        bakeMutex = bakeMutex,
        frameOperationMutex = frameOperationMutex,
        getState = { currentState },
        updateState = { updateState(it) },
        getBaseBitmap = { baseBitmap },
        setBaseBitmap = { baseBitmap = it },
        activeLayerId = { activeLayerId() },
        composeFrame = { frame, layerId, bitmap -> composeFrame(frame, bitmap, layerId) },
        history = history,
        redoHistory = redoHistory,
        acquireBitmap = { w, h -> acquireBitmap(w, h) },
        releaseBitmap = { b -> releaseBitmap(b) },
        onFrameBaked = { layerId, bitmap ->
            val boneId = currentState.currentFrameLayers().find { it.id == layerId }?.boundBoneId
            if (boneId != null) actorAuthoring.autoBindLayerContent(boneId, bitmap)
        }
    )

    private val frameManager = FrameManager(
        pendingOperations = pendingOperations,
        scope = viewModelScope,
        dispatcher = engineDispatcher,
        cacheManager = cacheManager,
        frameOperationMutex = frameOperationMutex,
        getState = { currentState },
        updateState = { updateState(it) },
        sendEffect = { sendEffect(it) },
        loadFrameInternal = { loadFrameInternal(it) },
        // Local JVM tests intentionally construct this ViewModel without Android's
        // bitmap runtime. Frame state transitions remain testable without invoking
        // Bitmap.createBitmap from the android.jar stub.
        finalizeFrame = { if (shouldInitialize && history.isNotEmpty()) drawingManager.rebuildCurrentBitmap() },
        clearHistory = {
            history.clear()
            redoHistory.clear()
        },
        commitFrameStructure = { inserted, deleted, change ->
            withContext(Dispatchers.Main.immediate) {
                sceneEffects.clear()
                inserted?.let { frameReferences.insert(it) }
                deleted.forEach { frameReferences.delete(it) }
                updateState { before ->
                    val next = change(before.copy(packTags = com.donnclab.kinetiframe.ui.packs.reindexPackTags(before.packTags, inserted, deleted)))
                    next.copy(layers = next.layers.copy(linkedCels = cacheManager.exposures(), linkedEditTarget = null),
                        project = next.project.copy(cameraShots = com.donnclab.kinetiframe.engine.camera.CameraFrameStructure.reindex(
                            before.project.cameraShots, inserted, deleted)))
                }
            }
        }
    )

    private val layerManager = LayerManager(
        updateState = { updateState(it) },
        // A missing PNG means a transparent layer. Eager blank initialization wasted RAM and
        // could overwrite a first stroke if its queued job completed after the user's drawing.
        refreshComposite = { refreshComposite() }
    )

    private val celHoldManager = CelHoldManager(viewModelScope, engineDispatcher, cacheManager,
        frameOperationMutex, { currentState }, { updateState(it) }, { sendEffect(it) },
        { if (history.isNotEmpty()) drawingManager.rebuildCurrentBitmap() },
        { history.clear(); redoHistory.clear() }, { loadFrameInternal(it) }, pendingOperations)

    private val selectionManager = SelectionManager(
        pendingOperations = pendingOperations,
        scope = viewModelScope,
        dispatcher = engineDispatcher,
        frameOperationMutex = frameOperationMutex,
        getState = { currentState },
        updateState = { updateState(it) },
        rebuildCurrentBitmap = { drawingManager.rebuildCurrentBitmap() },
        snapshotActiveLayer = {
            cacheManager.getFrame(activeLayerId(), currentState.animation.currentFrame)
                ?.copy(Bitmap.Config.ARGB_8888, true)
        },
        setBaseBitmap = { baseBitmap = it },
        history = history,
        redoHistory = redoHistory,
        trimHistory = { drawingManager.trimHistory() }
    )

    private val diagnosticReports = com.donnclab.kinetiframe.data.diagnostics.CrashReportStore(diagnosticsDir)
    private fun recordPlaybackFailure(error: Throwable, operation: String) {
        android.util.Log.e("KinetiFramePlayback", operation, error)
        val state = currentState
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { diagnosticReports.record(error,
                "$operation; frame=${state.animation.playbackFrame}; fps=${state.project.fps}; " +
                    "stage=${state.project.stage.width}x${state.project.stage.height}") }
        }
    }
    val audioPlaybackManager = com.donnclab.kinetiframe.domain.manager.AudioPlaybackManager(
        getState = { currentState }, scope = viewModelScope,
        onAudioError = { recordPlaybackFailure(it, "Audio playback failed") })
    private val audioManager = com.donnclab.kinetiframe.domain.manager.AudioManager(
        getState = { currentState },
        updateState = { updateState(it) }
    )

    /** Public (like [audioPlaybackManager]) so EditorScreen/CanvasArea can pull the current
     *  video reference frame straight from it on every recomposition, the same way it already
     *  reads [audioPlaybackManager] indirectly through the playback callbacks below. */
    val videoFrameProvider = com.donnclab.kinetiframe.domain.manager.VideoFrameProvider(getState = { currentState })
    private val videoTrackManager = com.donnclab.kinetiframe.domain.manager.VideoTrackManager(
        getState = { currentState },
        updateState = { updateState(it) }
    )

    private val playbackManager = PlaybackManager(
        scope = viewModelScope,
        engineDispatcher = engineDispatcher,
        engine = engine,
        cacheManager = cacheManager,
        updateState = { updateState(it) },
        loadFrame = { loadFrame(it) },
        composeFrame = { frame, bitmap -> composeFrame(frame, bitmap) },
        activeLayerId = { activeLayerId() },
        onFrameChanged = { frame -> audioPlaybackManager.onFrameChanged(frame) },
        onPlaybackStateChanged = { playing -> audioPlaybackManager.onPlaybackStateChanged(playing) },
        onPlaybackError = { error ->
            recordPlaybackFailure(error, "Frame playback failed")
            sendEffect(StudioEffect.ShowError("Playback stopped: ${error.message ?: error.javaClass.simpleName}"))
        }
    )

    private val viewManager = ViewManager(
        updateState = { updateState(it) }
    )

    private val backgroundManager = BackgroundManager(
        scope = viewModelScope,
        dispatcher = Dispatchers.IO,
        assets = projectAssetManager,
        storageDir = File(filesDir, "project-assets"),
        getState = { currentState },
        updateState = { updateState(it) },
        onError = { sendEffect(StudioEffect.ShowError(it)) },
        pendingOperations = pendingOperations
    )

    /** Everything about authoring an actor -- rig setup, pose/motion rehearsal, preset
     *  auditioning, sample artwork, pivot placement -- lives behind this one facade rather than
     *  five separate manager fields here. */
    private val actorAuthoring = ActorAuthoringCoordinator(
        scope = viewModelScope,
        rigAssetManager = rigAssetManager,
        getState = { currentState },
        updateState = { updateState(it) },
        sendEffect = { sendEffect(it) }
    )
    private val actorPlacementManager = ActorPlacementManager({ currentState }, { updateState(it) })

    private val textManager = TextManager(
        updateState = { updateState(it) }
    )

    init {
        backgroundManager.restore()
        viewModelScope.launch {
            runCatching { loadActorPresets() }
                .onSuccess { presets -> updateState { it.copy(actor = it.actor.copy(presets = presets)) } }
                .onFailure { sendEffect(StudioEffect.ShowError("Actor presets could not load: ${it.message}")) }
        }
        if (shouldInitialize) {
            pendingOperations.launch(viewModelScope, engineDispatcher) {
                // Reopening warms a durable document; it must never delete saved frame PNGs.
                frameOperationMutex.withLock { loadFrameInternal(0) }
            }
        }

        // MediaPlayer requires creation and every access on the same Looper thread. The
        // clock emits off-main, but playback state/audio controls belong to Main.
        viewModelScope.launch {
            engine.frameTicks.collect {
                // A buffered clock tick can arrive after Pause. It is not a Next Frame intent.
                if (currentState.animation.isPlaying) playbackManager.incrementFrame(currentState)
            }
        }
    }

    private val sceneEffects = com.donnclab.kinetiframe.domain.manager.SceneEffectsManager({ currentState }, { updateState(it) })

    override fun handleIntent(intent: StudioIntent) {
        if (intent is StudioIntent.Audio && intent !is StudioIntent.Audio.SetFades && intent !is StudioIntent.Audio.Crossfade && intent !is StudioIntent.Audio.VoiceStyle ||
            intent is StudioIntent.Video && intent !is StudioIntent.Video.Split && intent !is StudioIntent.Video.Framing) sceneEffects.clear()
        // Do not carry floating pixels or detached text into another frame/layer/history session.
        if (currentState.selection.selectedContent != null &&
            (intent is StudioIntent.Frame || intent is StudioIntent.PackItem.Add || intent is StudioIntent.Layer || intent is StudioIntent.Playback ||
                intent is StudioIntent.Draw || intent is StudioIntent.ActorPlacement || intent is StudioIntent.Text ||
                intent is StudioIntent.Camera || intent is StudioIntent.Video || intent is StudioIntent.Audio)) {
            viewModelScope.launch {
                selectionManager.commitSelection().join()
                handleIntent(intent)
            }
            return
        }
        when (intent) {
            is StudioIntent.Camera -> {
                playbackManager.pause(currentState)
                runCatching {
                    when (intent) {
                        is StudioIntent.Camera.Put -> sceneEffects.put(intent.shot)
                        is StudioIntent.Camera.Remove -> sceneEffects.remove(intent.id)
                        StudioIntent.Camera.Undo -> sceneEffects.undo()
                        StudioIntent.Camera.Redo -> sceneEffects.redo()
                    }
                }.onFailure { error -> viewModelScope.launch { sendEffect(StudioEffect.ShowError(error.message ?: "Could not edit camera")) } }
            }
            is StudioIntent.PackItem -> when (intent) {
                is StudioIntent.PackItem.SetTag -> updateState { it.copy(packTags = it.packTags + (intent.index to intent.tag)) }
                is StudioIntent.PackItem.Add -> frameManager.addFrameAt(intent.index, intent.tag)
            }
            is StudioIntent.ActorPlacement -> when (intent) {
                is StudioIntent.ActorPlacement.Add -> actorPlacementManager.add(intent.reference)
                is StudioIntent.ActorPlacement.Duplicate -> actorPlacementManager.duplicate(intent.placementId)
                is StudioIntent.ActorPlacement.Remove -> actorPlacementManager.remove(intent.placementId)
                is StudioIntent.ActorPlacement.UpdateTransform -> actorPlacementManager.updateTransform(intent.placementId, intent.transform)
                is StudioIntent.ActorPlacement.BringToFront -> actorPlacementManager.bringToFront(intent.placementId)
                is StudioIntent.ActorPlacement.SendToBack -> actorPlacementManager.sendToBack(intent.placementId)
                is StudioIntent.ActorPlacement.Trim -> actorPlacementManager.trim(intent.placementId, intent.startFrame, intent.durationFrames)
                is StudioIntent.ActorPlacement.Move -> actorPlacementManager.move(intent.placementId, intent.startFrame)
                is StudioIntent.ActorPlacement.SwitchPerformance -> actorPlacementManager.switchPerformance(
                    intent.placementId, intent.performanceId, intent.actorRevisionId, intent.label, intent.durationFrames)
                is StudioIntent.ActorPlacement.Split -> actorPlacementManager.split(intent.placementId, intent.atFrame)
            }
            is StudioIntent.Tracks -> when (intent) {
                is StudioIntent.Tracks.ToggleGroup -> updateState { state ->
                    val collapsed = state.tracks.collapsedGroups
                    state.copy(tracks = state.tracks.copy(collapsedGroups =
                        if (intent.group in collapsed) collapsed - intent.group else collapsed + intent.group))
                }
            }
            is StudioIntent.ActorPreset -> actorAuthoring.handlePreset(intent)
            is StudioIntent.ActorMotion -> actorAuthoring.handleMotion(intent)
            is StudioIntent.Actor -> actorAuthoring.handleActor(intent)
            is StudioIntent.Playback -> handlePlaybackIntent(intent)
            is StudioIntent.Draw -> handleDrawIntent(intent)
            is StudioIntent.Layer -> handleLayerIntent(intent)
            is StudioIntent.Frame -> handleFrameIntent(intent)
            is StudioIntent.View -> handleViewIntent(intent)
            is StudioIntent.OnionSkin -> handleOnionSkinIntent(intent)
            is StudioIntent.Rigging -> handleRiggingIntent(intent)
            is StudioIntent.Text -> handleTextIntent(intent)
            is StudioIntent.Selection -> handleSelectionIntent(intent)
            is StudioIntent.Fill -> handleFillIntent(intent)
            is StudioIntent.Construction -> handleConstructionShapeIntent(intent)
            is StudioIntent.Background -> backgroundManager.handle(intent)
            is StudioIntent.Audio -> when (intent) {
                is StudioIntent.Audio.VoiceStyle -> sceneEffects.voiceStyle(intent.clipId, intent.expectedAssetId, intent.effect, intent.proxy)
                is StudioIntent.Audio.Crossfade -> sceneEffects.crossfade(intent.clipId, intent.nextClipId, intent.frames)
                is StudioIntent.Audio.SetFades -> sceneEffects.fades(intent.clipId, intent.fadeInFrames, intent.fadeOutFrames)
                is StudioIntent.Audio.Add -> audioManager.add(intent.placement, intent.asset)
                is StudioIntent.Audio.Remove -> audioManager.remove(intent.clipId)
                is StudioIntent.Audio.Trim -> audioManager.trim(intent.clipId, intent.startFrame, intent.durationFrames)
                is StudioIntent.Audio.Move -> audioManager.move(intent.clipId, intent.trackIndex, intent.startFrame)
                is StudioIntent.Audio.SetVolume -> audioManager.setVolume(intent.clipId, intent.volume)
                is StudioIntent.Audio.SetTrackMuted -> audioManager.setTrackMuted(intent.trackIndex, intent.muted)
                is StudioIntent.Audio.SetTrackLocked -> audioManager.setTrackLocked(intent.trackIndex, intent.locked)
                is StudioIntent.Audio.Duplicate -> audioManager.duplicate(intent.clipId)
                is StudioIntent.Audio.Split -> audioManager.split(intent.clipId, intent.atFrame)
            }
            is StudioIntent.Video -> when (intent) {
                is StudioIntent.Video.Framing -> sceneEffects.videoFraming(intent.clipId, intent.fit, intent.cropX, intent.cropY)
                is StudioIntent.Video.Split -> sceneEffects.splitVideo(intent.clipId, intent.atFrame)
                is StudioIntent.Video.DetachAudio -> videoTrackManager.detachAudio(intent.clipId)
                is StudioIntent.Video.Add -> { videoTrackManager.add(intent.placement, intent.asset, intent.sourceAudio) }
                is StudioIntent.Video.Remove -> {
                    if (intent.clipId in currentState.selection.selectedContent?.videos.orEmpty()) viewModelScope.launch {
                        selectionManager.cancelSelection().join()
                        videoTrackManager.remove(intent.clipId)
                    } else videoTrackManager.remove(intent.clipId)
                }
                is StudioIntent.Video.Select -> {
                    currentState.project.videoClips[intent.clipId]?.let { clip ->
                        playbackManager.pause(currentState)
                        if (currentState.animation.currentFrame !in clip.timelineStartFrame until clip.timelineEndFrame)
                            playbackManager.setCurrentFrame(clip.timelineStartFrame, currentState.animation.totalFrames)
                        selectionManager.selectVideo(intent.clipId)
                    }
                }
                is StudioIntent.Video.Trim -> {
                    if (intent.clipId in currentState.selection.selectedContent?.videos.orEmpty()) viewModelScope.launch {
                        selectionManager.commitSelection().join()
                        videoTrackManager.trim(intent.clipId, intent.timelineStartFrame, intent.timelineEndFrame, intent.videoSourceOffsetSeconds)
                    } else videoTrackManager.trim(intent.clipId, intent.timelineStartFrame, intent.timelineEndFrame, intent.videoSourceOffsetSeconds)
                }
                is StudioIntent.Video.SetTrackOpacity -> videoTrackManager.setTrackOpacity(intent.opacity)
            }
        }
    }

    private fun handlePlaybackIntent(intent: StudioIntent.Playback) {
        if (currentState.scene.studioMode != StudioMode.RASTER_DRAWING) return
        when (intent) {
            StudioIntent.Playback.Play -> playbackManager.play(currentState)
            StudioIntent.Playback.Pause -> playbackManager.pause(currentState)
            StudioIntent.Playback.NextFrame -> playbackManager.incrementFrame(currentState)
            StudioIntent.Playback.PreviousFrame -> playbackManager.decrementFrame(currentState)
            is StudioIntent.Playback.SetFps -> playbackManager.setFps(intent.fps)
            is StudioIntent.Playback.SetCurrentFrame -> {
                if (currentState.animation.isPlaying) playbackManager.pause(currentState)
                playbackManager.setCurrentFrame(intent.frame, currentState.animation.totalFrames)
            }
        }
    }

    private fun handleDrawIntent(intent: StudioIntent.Draw) {
        if (currentState.actor.isPerformanceAuthoring) return
        when (intent) {
            is StudioIntent.Draw.BakeStroke -> {
                val targetLayerId = intent.targetBoneId?.let(::selectActorArtworkLayer)
                drawingManager.bakeStroke(intent.points, intent.brush, targetLayerId)
            }
            is StudioIntent.Draw.SelectTool -> {
                val rigTool = intent.tool in setOf(DrawingTool.LASSO_CUT, DrawingTool.BONE, DrawingTool.RIG_SELECT) ||
                    (intent.tool == DrawingTool.LASSO && currentState.scene.studioMode == StudioMode.RIG_SETUP)
                if (rigTool != (currentState.scene.studioMode != StudioMode.RASTER_DRAWING)) return
                updateState { it.copy(
                    tool = it.tool.copy(activeTool = intent.tool, constructionShape = null),
                    fill = it.fill.copy(previewActive = false, pendingTap = null, previewBitmap = null)
                ) }
            }
            is StudioIntent.Draw.SelectConstructionShape -> updateState { state ->
                state.copy(canvas = state.canvas.copy(rulerMode = RulerMode.NONE, rulerGuide = null), tool = state.tool.copy(
                    activeTool = DrawingTool.BRUSH,
                    constructionShape = intent.shape
                ))
            }
            is StudioIntent.Draw.SetBrushColor -> updateActiveToolConfig { it.copy(color = intent.color) }
            is StudioIntent.Draw.SetBrushSize -> updateActiveToolConfig { it.copy(size = intent.size.coerceIn(1f, 100f)) }
            is StudioIntent.Draw.SelectBrush -> updateToolConfig(DrawingTool.BRUSH) { it.copy(brushId = intent.brushId) }
            is StudioIntent.Draw.SetEraserSize -> updateToolConfig(DrawingTool.ERASER) { it.copy(size = intent.size.coerceIn(1f, 100f)) }
            is StudioIntent.Draw.SetEraserType -> updateToolConfig(DrawingTool.ERASER) { it.copy(eraserType = intent.type) }
            StudioIntent.Draw.ClearAllHandwriting -> drawingManager.clearAllHandwriting()
            is StudioIntent.Draw.SetBrushOpacity -> updateActiveToolConfig { it.copy(opacity = intent.opacity.coerceIn(0f, 1f)) }
            is StudioIntent.Draw.SetBrushStrength -> updateActiveToolConfig { it.copy(strength = intent.strength.coerceIn(0f, 1f)) }
            is StudioIntent.Draw.SetEraserCursor -> updateState { it.copy(tool = it.tool.copy(eraserCursor = intent.offset)) }
            StudioIntent.Draw.Undo -> drawingManager.undo()
            StudioIntent.Draw.Redo -> drawingManager.redo()
            is StudioIntent.Draw.DrawStroke -> { /* UI handle */ }
        }
    }

    private fun updateActiveToolConfig(block: (ToolConfig) -> ToolConfig) {
        updateState { state ->
            val toolState = state.tool
            val activeTool = toolState.activeTool
            if (toolState.constructionShape != null) state.copy(tool = toolState.copy(constructionConfig = block(toolState.constructionConfig)))
            else state.copy(tool = toolState.copy(configs = toolState.configs + (activeTool to block(toolState.configs[activeTool] ?: ToolConfig()))))
        }
    }

    private fun updateToolConfig(tool: DrawingTool, block: (ToolConfig) -> ToolConfig) {
        updateState { state ->
            val toolState = state.tool
            if (toolState.constructionShape != null && tool == DrawingTool.BRUSH) state.copy(tool = toolState.copy(constructionConfig = block(toolState.constructionConfig)))
            else state.copy(tool = toolState.copy(configs = toolState.configs + (tool to block(toolState.configs[tool] ?: ToolConfig()))))
        }
    }

    private fun handleLayerIntent(intent: StudioIntent.Layer) {
        when (intent) {
            is StudioIntent.Layer.SelectLayer -> {
                if (intent.index in currentState.currentFrameLayers().indices) {
                    if (currentState.scene.studioMode != StudioMode.RASTER_DRAWING) {
                        actorAuthoring.cancelInFlightAuthoring()
                    }
                    updateState { it.copy(layers = it.layers.copy(activeLayerIndex = intent.index)) }
                    loadFrame(currentState.animation.currentFrame)
                }
            }
            is StudioIntent.Layer.AddLayer -> layerManager.addLayer(intent.type)
            is StudioIntent.Layer.RemoveLayer -> layerManager.removeLayer(intent.index)
            is StudioIntent.Layer.ReorderLayer -> layerManager.reorderLayer(intent.fromIndex, intent.toIndex)
            is StudioIntent.Layer.SetLayerVisibility -> layerManager.setLayerVisibility(intent.index, intent.visible)
            is StudioIntent.Layer.ToggleLayerLock -> layerManager.toggleLayerLock(intent.index)
            is StudioIntent.Layer.SetLayerOpacity -> layerManager.setLayerOpacity(intent.index, intent.opacity)
            is StudioIntent.Layer.RenameLayer -> layerManager.renameLayer(intent.index, intent.name)
        }
    }

    private fun handleFrameIntent(intent: StudioIntent.Frame) {
        when (intent) {
            StudioIntent.Frame.AddFrame -> frameManager.addFrame()
            is StudioIntent.Frame.DeleteFrame -> frameManager.deleteFrame(intent.index)
            is StudioIntent.Frame.DuplicateFrame -> frameManager.duplicateFrame(intent.index)
            is StudioIntent.Frame.CopyFrame -> frameManager.copyFrame(intent.index)
            is StudioIntent.Frame.CopyFrames -> frameManager.copyFrames(intent.indices)
            is StudioIntent.Frame.PasteFrame -> frameManager.pasteFrame(intent.index, intent.before)
            is StudioIntent.Frame.PasteFrames -> frameManager.pasteFrames(intent.index, intent.before)
            is StudioIntent.Frame.DeleteFrames -> frameManager.deleteFrames(intent.indices)
            is StudioIntent.Frame.AddFrameAt -> frameManager.addFrameAt(intent.index)
            is StudioIntent.Frame.HoldDrawing -> celHoldManager.hold(intent.first, intent.last)
            is StudioIntent.Frame.EditDrawingScope -> celHoldManager.editScope(intent.index, intent.allLinked)
            is StudioIntent.Frame.UnlinkDrawing -> celHoldManager.unlink(intent.index)
        }
    }

    private fun handleViewIntent(intent: StudioIntent.View) {
        when (intent) {
            is StudioIntent.View.SetGridSize -> viewManager.setGridSize(intent.size)
            is StudioIntent.View.SetGridSpacing -> viewManager.setGridSpacing(intent.spacingDp)
            is StudioIntent.View.SetGridEnabled -> viewManager.setGridEnabled(intent.enabled)
            is StudioIntent.View.SetGridOpacity -> viewManager.setGridOpacity(intent.opacity)
            is StudioIntent.View.SetGridSpacingVertical -> viewManager.setGridSpacingVertical(intent.spacing)
            is StudioIntent.View.SetGridSpacingHorizontal -> viewManager.setGridSpacingHorizontal(intent.spacing)
            StudioIntent.View.ResetGridSettings -> viewManager.resetGridSettings()
            is StudioIntent.View.SetRulerMode -> viewManager.setRulerMode(intent.mode)
            is StudioIntent.View.UpdateRulerGuide -> viewManager.updateRulerGuide(intent.guide)
            is StudioIntent.View.UpdateCanvasTransform -> viewManager.updateCanvasTransform(intent.matrix)
            StudioIntent.View.ResetCanvasTransform -> viewManager.resetCanvasTransform()
            StudioIntent.View.ToggleSafeFrame -> viewManager.toggleSafeFrame()
            is StudioIntent.View.SetViewportSize -> viewManager.setViewportSize(intent.width, intent.height)
            is StudioIntent.View.UpdateTransform -> viewManager.updateTransform(intent.panDelta, intent.zoomDelta, intent.rotationDelta, intent.pivot)
            is StudioIntent.View.SetDockPosition -> viewManager.setDockPosition(intent.edge, intent.offset)
        }
    }

    private fun handleOnionSkinIntent(intent: StudioIntent.OnionSkin) {
        when (intent) {
            is StudioIntent.OnionSkin.SetEnabled -> updateState { it.copy(onionSkin = it.onionSkin.copy(enabled = intent.enabled)) }
            is StudioIntent.OnionSkin.SetFramesBefore -> updateState { it.copy(onionSkin = it.onionSkin.copy(depthBefore = intent.frames.coerceIn(0, 4))) }
            is StudioIntent.OnionSkin.SetFramesAfter -> updateState { it.copy(onionSkin = it.onionSkin.copy(depthAfter = intent.frames.coerceIn(0, 4))) }
            is StudioIntent.OnionSkin.SetColorBefore -> updateState { it.copy(onionSkin = it.onionSkin.copy(colorBefore = intent.color)) }
            is StudioIntent.OnionSkin.SetColorAfter -> updateState { it.copy(onionSkin = it.onionSkin.copy(colorAfter = intent.color)) }
            is StudioIntent.OnionSkin.SetOpacity -> updateState { it.copy(onionSkin = it.onionSkin.copy(opacity = intent.opacity.coerceIn(0f, 1f))) }
            StudioIntent.OnionSkin.Reset -> updateState { it.copy(onionSkin = OnionSkinState()) }
        }
    }

    private fun handleRiggingIntent(intent: StudioIntent.Rigging) {
        if (currentState.actor.isPerformanceAuthoring && intent !is StudioIntent.Rigging.SelectBone &&
            intent != StudioIntent.Rigging.ToggleSkeletonOverlay &&
            intent != StudioIntent.Rigging.SetStudioMode(StudioMode.ANIMATE_IK)) return
        when (intent) {
            is StudioIntent.Rigging.SetStudioMode -> {
                // A project-launched Performance Stage session may record motion only. The
                // actor revision it renders is immutable so prior gestures remain reproducible.
                if (currentState.actor.isPerformanceAuthoring && intent.mode != StudioMode.ANIMATE_IK) return
                actorAuthoring.cancelInFlightAuthoring()
                if (intent.mode == StudioMode.RASTER_DRAWING) actorAuthoring.clearHistory()
                if (intent.mode != StudioMode.RASTER_DRAWING) playbackManager.pause(currentState)
                updateState { state ->
                val layers = state.currentFrameLayers()
                val entering = state.scene.studioMode == StudioMode.RASTER_DRAWING && intent.mode != StudioMode.RASTER_DRAWING
                val leaving = intent.mode == StudioMode.RASTER_DRAWING
                val previousLayerId = layers.getOrNull(state.layers.activeLayerIndex)?.id
                val destination = if (leaving) state.actor.returnLayerId else if (entering) state.actor.lastActorLayerId else previousLayerId
                val destinationIndex = layers.indexOfFirst { it.id == destination }.takeIf { it >= 0 } ?: state.layers.activeLayerIndex
                state.copy(
                    layers = state.layers.copy(activeLayerIndex = destinationIndex),
                    actor = state.actor.copy(showSource = intent.mode != StudioMode.ANIMATE_IK,
                        returnLayerId = if (entering) previousLayerId else state.actor.returnLayerId,
                        lastActorLayerId = if (leaving) previousLayerId else state.actor.lastActorLayerId),
                    scene = state.scene.copy(studioMode = intent.mode),
                    tool = if (intent.mode == StudioMode.RASTER_DRAWING) {
                        state.tool.copy(activeTool = DrawingTool.BRUSH)
                    } else {
                        state.tool.copy(activeTool = DrawingTool.RIG_SELECT, constructionShape = null)
                    }
                )
                }
                if (intent.mode == StudioMode.RASTER_DRAWING && shouldInitialize) loadFrame(currentState.animation.currentFrame)
            }
            is StudioIntent.Rigging.PreparePivotPlacement -> actorAuthoring.preparePivotPlacement(intent.sourceFrame, intent.lassoPath, intent.dilationRadius)
            is StudioIntent.Rigging.MovePivot -> actorAuthoring.updatePivot(intent.x, intent.y)
            is StudioIntent.Rigging.AddBoneAt -> actorAuthoring.addBoneAt(intent.x, intent.y)
            is StudioIntent.Rigging.SelectBone -> {
                if (currentState.scene.pivotPlacement != null && currentState.scene.pivotPlacement?.targetBoneId != intent.id) actorAuthoring.cancelPivot()
                val boneId = intent.id
                // WHY this is not gated to RIG_SETUP: RIG_SETUP is the *pivot-adjust* tool state
                // (the Bone button); painting happens in RASTER_DRAWING. Arming the part's layer
                // only in RIG_SETUP meant the blueprint's core flow -- "tap the Left Forearm slot,
                // paint over its bone line, engine binds silently" -- could never fire from the
                // brush, because the layer carrying `boundBoneId` (the trigger for
                // RiggingManager.autoBindLayerContent on every bake) was never made active while
                // drawing. Selecting a part now arms and focuses its layer in either authoring
                // mode; ANIMATE_IK stays excluded because posing must never mint raster layers,
                // and the actorDefinition guard keeps a plain FxF project from doing so at all.
                if (boneId != null) selectActorArtworkLayer(boneId)
                updateState { state ->
                state.copy(scene = state.scene.copy(selectedBoneId = boneId))
                }
            }
            StudioIntent.Rigging.ConfirmPivot -> actorAuthoring.confirmPivot()
            StudioIntent.Rigging.CancelPivot -> actorAuthoring.cancelPivot()
            StudioIntent.Rigging.ToggleSkeletonOverlay -> updateState { state ->
                state.copy(scene = state.scene.copy(skeletonOverlayVisible = !state.scene.skeletonOverlayVisible))
            }
        }
    }

    private fun handleTextIntent(intent: StudioIntent.Text) {
        when (intent) {
            is StudioIntent.Text.AddText -> textManager.addText(intent.text, intent.x, intent.y)
            is StudioIntent.Text.UpdateText -> textManager.updateText(intent.id, intent.text)
            is StudioIntent.Text.SelectTextNode -> textManager.selectTextNode(intent.id)
            is StudioIntent.Text.DeleteTextNode -> textManager.deleteTextNode(intent.id)
            is StudioIntent.Text.SetTextToolOptions -> updateState { it.copy(tool = it.tool.copy(textToolOptions = intent.options)) }
            is StudioIntent.Text.UpdateTextNode -> textManager.updateTextNode(intent.node)
            is StudioIntent.Text.TriggerEditDialog -> {
                // The node may be detached from SceneState while SelectionManager owns
                // it for a transform. Both states represent the same editable node.
                val node = (currentState.scene.nodes[intent.id]
                    ?: currentState.selection.selectedContent?.nodes?.get(intent.id)) as? TextNode ?: return
                sendEffect(StudioEffect.ShowTextEditDialog(intent.id, node.text))
            }
            is StudioIntent.Text.TriggerAddTextDialog -> {
                sendEffect(StudioEffect.ShowTextAddDialog(intent.x, intent.y))
            }
        }
    }

    private fun handleSelectionIntent(intent: StudioIntent.Selection) {
        when (intent) {
            is StudioIntent.Selection.SetTool -> updateState { it.copy(selection = it.selection.copy(toolType = intent.type)) }
            is StudioIntent.Selection.SelectArea -> selectionManager.selectArea(intent.path, intent.rect, intent.density, intent.fontRegistry, intent.actors)
            is StudioIntent.Selection.SelectTextNode -> selectionManager.selectTextNode(intent.id, intent.density, intent.fontRegistry)
            is StudioIntent.Selection.SetActive -> {
                if (!intent.active && currentState.selection.active) {
                    selectionManager.commitSelection()
                } else {
                    updateState { it.copy(selection = it.selection.copy(active = intent.active, contextMenuVisible = if (intent.active) it.selection.contextMenuVisible else false)) }
                }
            }
            is StudioIntent.Selection.UpdateTransform -> updateState { it.copy(selection = it.selection.copy(transform = intent.matrix)) }
            is StudioIntent.Selection.UpdatePivot -> updateState { it.copy(selection = it.selection.copy(pivotOffset = intent.offset)) }
            is StudioIntent.Selection.SetContextMenuVisible -> updateState { it.copy(selection = it.selection.copy(contextMenuVisible = intent.visible)) }
            is StudioIntent.Selection.SetInteractionMode -> updateState {
                it.copy(selection = it.selection.copy(interactionMode = intent.mode, contextMenuVisible = false))
            }
            StudioIntent.Selection.Cut -> selectionManager.cutSelection()
            StudioIntent.Selection.Copy -> {
                if (currentState.selection.active) {
                    selectionManager.copySelection()
                } else {
                    frameManager.copyFrame(currentState.animation.currentFrame)
                }
            }
            StudioIntent.Selection.Paste -> {
                if (selectionManager.hasClipboardContent()) {
                    selectionManager.pasteSelection()
                } else {
                    frameManager.pasteFrame(currentState.animation.currentFrame, before = false)
                }
            }
            is StudioIntent.Selection.StampBitmap -> selectionManager.pasteBitmap(intent.bitmap, currentState.project.stage.width, currentState.project.stage.height, intent.centerX, intent.centerY)
            StudioIntent.Selection.Delete -> selectionManager.deleteSelection()
            StudioIntent.Selection.Cancel -> selectionManager.cancelSelection()
            is StudioIntent.Selection.SetAspectLock -> updateState { it.copy(selection = it.selection.copy(lockAspectRatio = intent.locked)) }
            is StudioIntent.Selection.SetSkewMode -> updateState { it.copy(selection = it.selection.copy(skewMode = intent.enabled)) }
            StudioIntent.Selection.ResetTransform -> updateState { it.copy(selection = it.selection.copy(transform = androidx.compose.ui.graphics.Matrix(), pivotOffset = androidx.compose.ui.geometry.Offset.Zero)) }
            is StudioIntent.Selection.SetDockPosition -> updateState { it.copy(canvas = it.canvas.copy(selectionDockOffset = intent.offset)) }
            is StudioIntent.Selection.SetPartialInclude -> updateState { it.copy(selection = it.selection.copy(partialInclude = intent.include)) }
        }
    }

    private val frameRequestGeneration = java.util.concurrent.atomic.AtomicLong()

    private fun loadFrame(index: Int) {
        val generation = frameRequestGeneration.incrementAndGet()
        pendingOperations.launch(viewModelScope, engineDispatcher) {
            frameOperationMutex.withLock {
                // A fast fling must not queue hundreds of obsolete disk decodes/composites.
                // Keep the mutex: the outgoing drawing still has to be committed safely.
                if (generation == frameRequestGeneration.get() && index == currentState.animation.currentFrame)
                    loadFrameInternal(index)
            }
        }
    }

    private suspend fun loadFrameInternal(index: Int) {
        if (history.isNotEmpty()) {
            drawingManager.rebuildCurrentBitmap()
        }
        cacheManager.resetEditScope()
        updateState { it.copy(layers = it.layers.copy(linkedEditTarget = null, linkedCels = cacheManager.exposures())) }

        val bitmap = cacheManager.getFrame(activeLayerId(), index)
        baseBitmap = withContext(engineDispatcher) {
            bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        }
        history.clear()
        redoHistory.clear()
        drawingManager.markLayerSession(activeLayerId(), index)

        val compositeBitmap = composeFrame(index, bitmap)
        updateState { if (it.animation.currentFrame == index) it.copy(currentBitmap = compositeBitmap) else it }
        
        // Predictive Loading
        cacheManager.getFrame(activeLayerId(), (index + 1) % currentState.animation.totalFrames)
        if (index > 0) {
            cacheManager.getFrame(activeLayerId(), index - 1)
        } else {
            cacheManager.getFrame(activeLayerId(), currentState.animation.totalFrames - 1)
        }
    }

    private fun handleFillIntent(intent: StudioIntent.Fill) {
        when (intent) {
            is StudioIntent.Fill.SetPendingTap -> {
                updateState { it.copy(fill = it.fill.copy(pendingTap = intent.offset, previewActive = intent.offset != null)) }
                if (intent.offset != null) {
                    generateFillPreview(intent.offset)
                }
            }
            is StudioIntent.Fill.SetDockPosition -> updateState { it.copy(fill = it.fill.copy(dockOffset = intent.offset)) }
            StudioIntent.Fill.Confirm -> {
                val tap = currentState.fill.pendingTap
                if (tap != null) {
                    drawingManager.bakeStroke(listOf(TouchPoint(tap.x, tap.y)))
                }
                updateState { it.copy(fill = it.fill.copy(previewActive = false, pendingTap = null, previewBitmap = null)) }
            }
            StudioIntent.Fill.Cancel -> {
                updateState { it.copy(fill = it.fill.copy(previewActive = false, pendingTap = null, previewBitmap = null)) }
            }
            is StudioIntent.Fill.SetPreviewBitmap -> updateState { it.copy(fill = it.fill.copy(previewBitmap = intent.bitmap)) }
        }
    }

    private fun handleConstructionShapeIntent(intent: StudioIntent.Construction) {
        when (intent) {
            is StudioIntent.Construction.SetPreview -> updateState {
                it.copy(constructionShape = it.constructionShape.copy(previewStrokes = intent.strokes, previewActive = intent.strokes.isNotEmpty()))
            }
            is StudioIntent.Construction.SetDockPosition -> updateState {
                it.copy(constructionShape = it.constructionShape.copy(dockOffset = intent.offset))
            }
            StudioIntent.Construction.Cancel -> updateState {
                it.copy(constructionShape = it.constructionShape.copy(previewStrokes = emptyList(), previewActive = false))
            }
            StudioIntent.Construction.Confirm -> {
                val preview = currentState.constructionShape.previewStrokes
                drawingManager.bakeConstructionShape(preview)
                updateState { it.copy(constructionShape = it.constructionShape.copy(previewStrokes = emptyList(), previewActive = false)) }
            }
        }
    }

    private fun generateFillPreview(offset: androidx.compose.ui.geometry.Offset) {
        pendingOperations.launch(viewModelScope, engineDispatcher) {
            val preview = drawingManager.generateFillPreview(offset)
            withContext(Dispatchers.Main) {
                handleIntent(StudioIntent.Fill.SetPreviewBitmap(preview))
            }
        }
    }

    private fun activeLayerId(): String =
        currentState.currentFrameLayers().getOrNull(currentState.layers.activeLayerIndex)?.id ?: "main"

    /** Ensures one freehand actor stroke has exactly one isolated raster destination. */
    private fun selectActorArtworkLayer(boneId: String): String? {
        if (currentState.scene.studioMode == StudioMode.ANIMATE_IK || currentState.actorDefinition() == null) return null
        val existingIndex = currentState.currentFrameLayers().indexOfFirst { it.boundBoneId == boneId }
        val layerId = if (existingIndex >= 0) {
            updateState { state -> state.copy(layers = state.layers.copy(activeLayerIndex = existingIndex)) }
            currentState.currentFrameLayers()[existingIndex].id
        } else {
            val boneName = currentState.actorDefinition()?.sceneGraph?.bones?.get(boneId)?.name
            layerManager.addLayer(LayerType.RASTER, boundBoneId = boneId, customName = boneName)
        }
        updateState { state -> state.copy(scene = state.scene.copy(selectedBoneId = boneId)) }
        return layerId
    }

    private suspend fun composeFrame(
        frameIndex: Int,
        activeLayerBitmap: Bitmap? = null,
        bitmapLayerId: String = activeLayerId()
    ): Bitmap? = withContext(engineDispatcher) {
        val layers = currentState.layersForFrame(frameIndex)
        if (layers.isEmpty()) return@withContext activeLayerBitmap
        val loaded = layers.associateWith { layer ->
            if (frameIndex == currentState.animation.currentFrame &&
                layer.id == bitmapLayerId && activeLayerBitmap != null
            ) activeLayerBitmap
            else cacheManager.getFrame(layer.id, frameIndex)
        }
        if (loaded.values.all { it == null }) return@withContext null
        val reference = loaded.values.firstOrNull { it != null } ?: return@withContext null
        val destination = acquireBitmap(reference.width, reference.height)
        LayerCompositor.compose(layers, frameIndex, destination) { layer, _ -> loaded[layer] }
        destination
    }

    private fun refreshComposite() {
        pendingOperations.launch(viewModelScope, engineDispatcher) {
            val frame = currentState.animation.currentFrame
            val activeBitmap = cacheManager.getFrame(activeLayerId(), frame)
            val composite = composeFrame(frame, activeBitmap)
            updateState { it.copy(currentBitmap = composite) }
        }
    }

    override fun onCleared() {
        frameReferences.clear()
        projectAssetManager.clear()
        engine.stop()
        audioPlaybackManager.release()
        videoFrameProvider.release()
        rigAssetManager.clear()
        while (true) {
            val bitmap = bitmapPool.poll() ?: break
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
