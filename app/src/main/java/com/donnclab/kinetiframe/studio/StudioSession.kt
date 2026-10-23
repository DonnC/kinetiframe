package com.donnclab.kinetiframe.studio

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.domain.ActorAuthoringCoordinator
import com.donnclab.kinetiframe.actors.domain.ActorPlacementManager
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.audio.AudioManager
import com.donnclab.kinetiframe.audio.AudioPlaybackManager
import com.donnclab.kinetiframe.core.document.LayerType
import com.donnclab.kinetiframe.studio.domain.AnimationEngine
import com.donnclab.kinetiframe.studio.domain.BackgroundManager
import com.donnclab.kinetiframe.studio.domain.CelHoldManager
import com.donnclab.kinetiframe.studio.domain.DrawingManager
import com.donnclab.kinetiframe.studio.domain.FrameManager
import com.donnclab.kinetiframe.studio.domain.HistoryItem
import com.donnclab.kinetiframe.studio.domain.LayerManager
import com.donnclab.kinetiframe.studio.domain.PendingOperations
import com.donnclab.kinetiframe.studio.domain.PlaybackManager
import com.donnclab.kinetiframe.studio.domain.SceneEffectsManager
import com.donnclab.kinetiframe.studio.domain.SelectionManager
import com.donnclab.kinetiframe.studio.domain.TextManager
import com.donnclab.kinetiframe.studio.domain.ViewManager
import com.donnclab.kinetiframe.studio.drawing.LayerCompositor
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.video.VideoFrameProvider
import com.donnclab.kinetiframe.video.VideoTrackManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Everything one open studio shares: the frame cache, undo history, bitmap pool and locks, the
 * managers that edit the project, and frame loading and composition. [StudioViewModel] owns one
 * session and routes intents to it through [StudioCanvasIntents] and [StudioSceneIntents].
 */
internal class StudioSession(
    private val store: StudioStore,
    private val directories: StudioDirectories,
    private val engineDispatcher: CoroutineDispatcher,
    val shouldInitialize: Boolean,
    private val loadActorPresets: suspend () -> List<com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset>,
    runtimeRigAssetManager: RigAssetManager?,
) {
    val engine = AnimationEngine(store.scope, engineDispatcher)
    val cacheManager = FrameCacheManager(directories.cache, store.scope).apply {
        restoreExposures(store.state.layers.linkedCels, store.state.animation.totalFrames)
    }
    val frameReferences = com.donnclab.kinetiframe.studio.reference.FrameReferenceSession()

    /** Supplied by Actor Studio when reopening a revision with persisted artwork. */
    val rigAssetManager = runtimeRigAssetManager ?: RigAssetManager()

    /**
     * Decoded bitmap for the project's persistent background image, if one is set. It is kept apart
     * from [rigAssetManager] because the background has one bitmap and no undo history.
     */
    val projectAssetManager = com.donnclab.kinetiframe.media.ProjectAssetManager()
    val bakeMutex = Mutex()
    val frameOperationMutex = Mutex()
    val pendingOperations = PendingOperations()
    val saveMutex = Mutex()
    fun launchSave(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) = store.scope.launch {
        val owner = this
        saveMutex.withLock { block(owner) }
    }

    /** Commit floating selection and await accepted raster/frame edits before publishing a save. */
    suspend fun prepareProjectSave(nodeDensity: Float): com.donnclab.kinetiframe.project.data.ProjectSaveDocument {
        pendingOperations.awaitIdle()
        selectionManager.commitSelection().join()
        pendingOperations.awaitIdle()
        return frameOperationMutex.withLock {
            // Accepted drawing/selection jobs have already baked their pixels. Rebuilding
            // an untouched layer here creates blank artwork and can rebind an actor skin.
            val state = store.state
            val stacks = state.layers.frameLayers + (state.animation.currentFrame to state.layers.layers)
            val document = com.donnclab.kinetiframe.project.data.ProjectSaveDocument(
                project = state.project.copy(
                    layers = stacks[0] ?: state.layers.layers,
                    nodes = state.scene.nodes.mapValues {
                        it.value.copyWithSelection(false)
                    },
                    nodeDensity = nodeDensity,
                ),
                frameLayers = stacks,
                totalFrames = state.animation.totalFrames,
                activeLayerIndex = state.layers.activeLayerIndex,
                linkedCels = cacheManager.exposures().filterKeys { id -> state.layers.layers.any { it.id == id } },
            )
            cacheManager.persistAllToDisk(stacks.values.flatten().map { it.id }.distinct(), document.totalFrames)
            document
        }
    }

    val bitmapPool = com.donnclab.kinetiframe.studio.render.BitmapPool()

    var history = mutableListOf<HistoryItem>()
    var redoHistory = mutableListOf<HistoryItem>()
    var baseBitmap: Bitmap? = null

    // Domain Managers
    val drawingManager = DrawingManager(
        pendingOperations = pendingOperations,
        scope = store.scope,
        dispatcher = engineDispatcher,
        cacheManager = cacheManager,
        bakeMutex = bakeMutex,
        frameOperationMutex = frameOperationMutex,
        getState = { store.state },
        updateState = { store.update(it) },
        getBaseBitmap = { baseBitmap },
        setBaseBitmap = { baseBitmap = it },
        activeLayerId = { activeLayerId() },
        composeFrame = { frame, layerId, bitmap -> composeFrame(frame, bitmap, layerId) },
        history = history,
        redoHistory = redoHistory,
        acquireBitmap = { w, h -> bitmapPool.acquire(w, h) },
        releaseBitmap = { b -> bitmapPool.release(b) },
        onFrameBaked = { layerId, bitmap ->
            val boneId = store.state.currentFrameLayers().find { it.id == layerId }?.boundBoneId
            if (boneId != null) actorAuthoring.autoBindLayerContent(boneId, bitmap)
        },
    )

    val frameManager = FrameManager(
        pendingOperations = pendingOperations,
        scope = store.scope,
        dispatcher = engineDispatcher,
        cacheManager = cacheManager,
        frameOperationMutex = frameOperationMutex,
        getState = { store.state },
        updateState = { store.update(it) },
        sendEffect = { store.send(it) },
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
                store.update { before ->
                    val next =
                        change(
                            before.copy(
                                packTags = com.donnclab.kinetiframe.library.ui.packs.reindexPackTags(
                                    before.packTags,
                                    inserted,
                                    deleted,
                                ),
                            ),
                        )
                    next.copy(
                        layers = next.layers.copy(linkedCels = cacheManager.exposures(), linkedEditTarget = null),
                        project = next.project.copy(
                            cameraShots = com.donnclab.kinetiframe.studio.camera.CameraFrameStructure.reindex(
                                before.project.cameraShots,
                                inserted,
                                deleted,
                            ),
                        ),
                    )
                }
            }
        },
    )

    val layerManager = LayerManager(
        updateState = { store.update(it) },
        // A missing PNG means a transparent layer. Eager blank initialization wasted RAM and
        // could overwrite a first stroke if its queued job completed after the user's drawing.
        refreshComposite = { refreshComposite() },
    )

    val celHoldManager = CelHoldManager(
        store.scope, engineDispatcher, cacheManager,
        frameOperationMutex, { store.state }, { store.update(it) }, { store.send(it) },
        { if (history.isNotEmpty()) drawingManager.rebuildCurrentBitmap() },
        {
            history.clear()
            redoHistory.clear()
        }, { loadFrameInternal(it) }, pendingOperations,
    )

    val selectionManager = SelectionManager(
        pendingOperations = pendingOperations,
        scope = store.scope,
        dispatcher = engineDispatcher,
        frameOperationMutex = frameOperationMutex,
        getState = { store.state },
        updateState = { store.update(it) },
        rebuildCurrentBitmap = { drawingManager.rebuildCurrentBitmap() },
        snapshotActiveLayer = {
            cacheManager.getFrame(activeLayerId(), store.state.animation.currentFrame)
                ?.copy(Bitmap.Config.ARGB_8888, true)
        },
        setBaseBitmap = { baseBitmap = it },
        history = history,
        redoHistory = redoHistory,
        trimHistory = { drawingManager.trimHistory() },
    )

    val diagnosticReports = com.donnclab.kinetiframe.diagnostics.CrashReportStore(directories.diagnostics)
    fun recordPlaybackFailure(error: Throwable, operation: String) {
        android.util.Log.e("KinetiFramePlayback", operation, error)
        val state = store.state
        store.scope.launch(Dispatchers.IO) {
            runCatching {
                diagnosticReports.record(
                    error,
                    "$operation; frame=${state.animation.playbackFrame}; fps=${state.project.fps}; " +
                        "stage=${state.project.stage.width}x${state.project.stage.height}",
                )
            }
        }
    }
    val audioPlaybackManager = com.donnclab.kinetiframe.audio.AudioPlaybackManager(
        getState = { store.state },
        scope = store.scope,
        onAudioError = { recordPlaybackFailure(it, "Audio playback failed") },
    )
    val audioManager = com.donnclab.kinetiframe.audio.AudioManager(
        getState = { store.state },
        updateState = { store.update(it) },
    )

    /** Public (like [audioPlaybackManager]) so EditorScreen/CanvasArea can pull the current
     *  video reference frame straight from it on every recomposition, the same way it already
     *  reads [audioPlaybackManager] indirectly through the playback callbacks below. */
    val videoFrameProvider = com.donnclab.kinetiframe.video.VideoFrameProvider(getState = { store.state })
    val videoTrackManager = com.donnclab.kinetiframe.video.VideoTrackManager(
        getState = { store.state },
        updateState = { store.update(it) },
    )

    val playbackManager = PlaybackManager(
        scope = store.scope,
        engineDispatcher = engineDispatcher,
        engine = engine,
        cacheManager = cacheManager,
        updateState = { store.update(it) },
        loadFrame = { loadFrame(it) },
        composeFrame = { frame, bitmap -> composeFrame(frame, bitmap) },
        activeLayerId = { activeLayerId() },
        onFrameChanged = { frame -> audioPlaybackManager.onFrameChanged(frame) },
        onPlaybackStateChanged = { playing -> audioPlaybackManager.onPlaybackStateChanged(playing) },
        onPlaybackError = { error ->
            recordPlaybackFailure(error, "Frame playback failed")
            store.send(StudioEffect.ShowError("Playback stopped: ${error.message ?: error.javaClass.simpleName}"))
        },
    )

    val viewManager = ViewManager(
        updateState = { store.update(it) },
    )

    val backgroundManager = BackgroundManager(
        scope = store.scope,
        dispatcher = Dispatchers.IO,
        assets = projectAssetManager,
        storageDir = File(directories.files, "project-assets"),
        getState = { store.state },
        updateState = { store.update(it) },
        onError = { store.send(StudioEffect.ShowError(it)) },
        pendingOperations = pendingOperations,
    )

    /** Rig setup, posing and motion, preset auditions, sample artwork and pivot placement. */
    val actorAuthoring = ActorAuthoringCoordinator(
        scope = store.scope,
        rigAssetManager = rigAssetManager,
        getState = { store.state },
        updateState = { store.update(it) },
        sendEffect = { store.send(it) },
    )
    val actorPlacementManager = ActorPlacementManager({ store.state }, { store.update(it) })

    val textManager = TextManager(
        updateState = { store.update(it) },
    )

    init {
        backgroundManager.restore()
        store.scope.launch {
            runCatching { loadActorPresets() }
                .onSuccess { presets -> store.update { it.copy(actor = it.actor.copy(presets = presets)) } }
                .onFailure { store.send(StudioEffect.ShowError("Actor presets could not load: ${it.message}")) }
        }
        if (shouldInitialize) {
            pendingOperations.launch(store.scope, engineDispatcher) {
                // Reopening warms a durable document; it must never delete saved frame PNGs.
                frameOperationMutex.withLock { loadFrameInternal(0) }
            }
        }

        // MediaPlayer requires creation and every access on the same Looper thread. The
        // clock emits off-main, but playback state/audio controls belong to Main.
        store.scope.launch {
            engine.frameTicks.collect {
                // A buffered clock tick can arrive after Pause. It is not a Next Frame intent.
                if (store.state.animation.isPlaying) playbackManager.incrementFrame(store.state)
            }
        }
    }

    val sceneEffects = com.donnclab.kinetiframe.studio.domain.SceneEffectsManager({ store.state }, { store.update(it) })

    val frameRequestGeneration = java.util.concurrent.atomic.AtomicLong()

    fun loadFrame(index: Int) {
        val generation = frameRequestGeneration.incrementAndGet()
        pendingOperations.launch(store.scope, engineDispatcher) {
            frameOperationMutex.withLock {
                // A fast fling must not queue hundreds of obsolete disk decodes/composites.
                // Keep the mutex: the outgoing drawing still has to be committed safely.
                if (generation == frameRequestGeneration.get() && index == store.state.animation.currentFrame) {
                    loadFrameInternal(index)
                }
            }
        }
    }

    suspend fun loadFrameInternal(index: Int) {
        if (history.isNotEmpty()) {
            drawingManager.rebuildCurrentBitmap()
        }
        cacheManager.resetEditScope()
        store.update {
            it.copy(layers = it.layers.copy(linkedEditTarget = null, linkedCels = cacheManager.exposures()))
        }

        val bitmap = cacheManager.getFrame(activeLayerId(), index)
        baseBitmap = withContext(engineDispatcher) {
            bitmap?.copy(Bitmap.Config.ARGB_8888, true)
        }
        history.clear()
        redoHistory.clear()
        drawingManager.markLayerSession(activeLayerId(), index)

        val compositeBitmap = composeFrame(index, bitmap)
        store.update { if (it.animation.currentFrame == index) it.copy(currentBitmap = compositeBitmap) else it }

        // Predictive Loading
        cacheManager.getFrame(activeLayerId(), (index + 1) % store.state.animation.totalFrames)
        if (index > 0) {
            cacheManager.getFrame(activeLayerId(), index - 1)
        } else {
            cacheManager.getFrame(activeLayerId(), store.state.animation.totalFrames - 1)
        }
    }

    fun generateFillPreview(offset: androidx.compose.ui.geometry.Offset) {
        pendingOperations.launch(store.scope, engineDispatcher) {
            val preview = drawingManager.generateFillPreview(offset)
            withContext(Dispatchers.Main) {
                store.update { it.reduceFillSetting(StudioIntent.Fill.SetPreviewBitmap(preview)) }
            }
        }
    }

    fun activeLayerId(): String =
        store.state.currentFrameLayers().getOrNull(store.state.layers.activeLayerIndex)?.id ?: "main"

    /** Ensures one freehand actor stroke has exactly one isolated raster destination. */
    fun selectActorArtworkLayer(boneId: String): String? {
        if (store.state.scene.studioMode == StudioMode.ANIMATE_IK || store.state.actorDefinition() == null) return null
        val existingIndex = store.state.currentFrameLayers().indexOfFirst { it.boundBoneId == boneId }
        val layerId = if (existingIndex >= 0) {
            store.update { state -> state.copy(layers = state.layers.copy(activeLayerIndex = existingIndex)) }
            store.state.currentFrameLayers()[existingIndex].id
        } else {
            val boneName = store.state.actorDefinition()?.sceneGraph?.bones?.get(boneId)?.name
            layerManager.addLayer(LayerType.RASTER, boundBoneId = boneId, customName = boneName)
        }
        store.update { state -> state.copy(scene = state.scene.copy(selectedBoneId = boneId)) }
        return layerId
    }

    suspend fun composeFrame(
        frameIndex: Int,
        activeLayerBitmap: Bitmap? = null,
        bitmapLayerId: String = activeLayerId(),
    ): Bitmap? = withContext(engineDispatcher) {
        val layers = store.state.layersForFrame(frameIndex)
        if (layers.isEmpty()) return@withContext activeLayerBitmap
        val loaded = layers.associateWith { layer ->
            if (frameIndex == store.state.animation.currentFrame &&
                layer.id == bitmapLayerId && activeLayerBitmap != null
            ) {
                activeLayerBitmap
            } else {
                cacheManager.getFrame(layer.id, frameIndex)
            }
        }
        if (loaded.values.all { it == null }) return@withContext null
        val reference = loaded.values.firstOrNull { it != null } ?: return@withContext null
        val destination = bitmapPool.acquire(reference.width, reference.height)
        LayerCompositor.compose(layers, frameIndex, destination) { layer, _ -> loaded[layer] }
        destination
    }

    fun refreshComposite() {
        pendingOperations.launch(store.scope, engineDispatcher) {
            val frame = store.state.animation.currentFrame
            val activeBitmap = cacheManager.getFrame(activeLayerId(), frame)
            val composite = composeFrame(frame, activeBitmap)
            store.update { it.copy(currentBitmap = composite) }
        }
    }

    /** Frees bitmaps, players and decoders. Called once, when the studio closes. */
    fun release() {
        frameReferences.clear()
        projectAssetManager.clear()
        engine.stop()
        audioPlaybackManager.release()
        videoFrameProvider.release()
        rigAssetManager.clear()
        bitmapPool.clear()
    }
}
