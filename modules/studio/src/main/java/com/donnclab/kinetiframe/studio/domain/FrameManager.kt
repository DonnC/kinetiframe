package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioFailure
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manager for frame operations.
 */
class FrameManager(
    runtime: StudioRuntime,
    private val cacheManager: FrameCacheManager,
    private val frameOperationMutex: Mutex,
    hooks: FrameHooks,
) {
    private val scope: CoroutineScope = runtime.scope
    private val dispatcher: CoroutineDispatcher = runtime.dispatcher
    private val getState: () -> StudioState = runtime.getState
    private val updateState: ((StudioState) -> StudioState) -> Unit = runtime.updateState
    private val pendingOperations: PendingOperations = runtime.pending
    private val sendEffect: (StudioEffect) -> Unit = hooks.sendEffect
    private val loadFrameInternal: suspend (Int) -> Unit = hooks.loadFrameInternal
    private val finalizeFrame: suspend (Int) -> Unit = hooks.finalizeFrame
    private val clearHistory: () -> Unit = hooks.clearHistory

    /** UI-only guides must be remapped in the same publication turn as the frame indices. */
    private val commitFrameStructure: suspend (Int?, List<Int>, (StudioState) -> StudioState) -> Unit =
        hooks.commitFrameStructure ?: { _, _, change -> updateState(change) }

    private data class ClipboardFrame(val layers: List<Layer>, val bitmaps: Map<String, Bitmap>)
    private var copiedFrame: ClipboardFrame? = null
    private var copiedFrames: List<ClipboardFrame> = emptyList()

    fun hasClipboardContent(): Boolean = copiedFrame != null || copiedFrames.isNotEmpty()

    fun addFrame() {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                // Read state after acquiring the lock: rapid taps must not insert repeatedly at
                // the same stale index or shift bitmaps under the wrong metadata.
                val currentState = getState()
                if (currentState.animation.totalFrames >= 1000) {
                    sendEffect(StudioEffect.ShowError(StudioFailure.FRAME_LIMIT))
                    return@withLock
                }
                val insertIndex = currentState.animation.currentFrame + 1
                finalizeFrame(currentState.animation.currentFrame)
                clearHistory()

                currentState.currentFrameLayers().forEach { layer ->
                    cacheManager.insertFrame(layer.id, insertIndex, currentState.animation.totalFrames)
                }

                commitFrameStructure(insertIndex, emptyList()) { state ->
                    val shiftedLayers = state.layers.frameLayers.mapKeys { (key, _) ->
                        if (key >=
                            insertIndex
                        ) {
                            key + 1
                        } else {
                            key
                        }
                    }.toMutableMap()
                    val shiftedTransforms = state.animation.frameTransforms.mapKeys { (key, _) ->
                        if (key >=
                            insertIndex
                        ) {
                            key + 1
                        } else {
                            key
                        }
                    }.toMutableMap()

                    // A frame owns new pixels, not a new layer stack. Keep the same layer IDs
                    // so onion skin and layer order remain stable across frame insertion.
                    val stack = state.currentFrameLayers()
                    shiftedLayers[insertIndex] = stack

                    state.copy(
                        animation = state.animation.copy(
                            totalFrames = state.animation.totalFrames + 1,
                            currentFrame = insertIndex,
                            playbackFrame = 0,
                            frameTransforms = shiftedTransforms,
                        ),
                        layers = state.layers.copy(
                            layers = stack,
                            frameLayers = shiftedLayers,
                            activeLayerIndex = state.layers.activeLayerIndex.coerceIn(0, stack.lastIndex),
                        ),
                        currentBitmap = null,
                    )
                }
                loadFrameInternal(insertIndex)
            }
        }
    }

    fun addFrameAt(index: Int, packTag: com.donnclab.kinetiframe.library.model.PackFrameTag? = null) {
        val currentState = getState()
        if (currentState.animation.totalFrames >= 1000) return
        val target = index.coerceIn(0, currentState.animation.totalFrames)
        if (packTag == null && target == currentState.animation.currentFrame + 1) return addFrame()
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                val target = index.coerceIn(0, currentState.animation.totalFrames)
                if (currentState.animation.totalFrames >= 1000) return@withLock
                finalizeFrame(currentState.animation.currentFrame)
                clearHistory()
                currentState.currentFrameLayers().map { it.id }.distinct().forEach { layerId ->
                    cacheManager.insertFrame(layerId, target, currentState.animation.totalFrames)
                }
                commitFrameStructure(target, emptyList()) { state ->
                    val shiftedLayers = state.layers.frameLayers.mapKeys { (key, _) ->
                        if (key >=
                            target
                        ) {
                            key + 1
                        } else {
                            key
                        }
                    }.toMutableMap()
                    val shiftedTransforms = state.animation.frameTransforms.mapKeys { (key, _) ->
                        if (key >=
                            target
                        ) {
                            key + 1
                        } else {
                            key
                        }
                    }.toMutableMap()
                    val stack = state.currentFrameLayers()
                    shiftedLayers[target] = stack
                    state.copy(
                        animation = state.animation.copy(
                            totalFrames = state.animation.totalFrames + 1,
                            currentFrame = target,
                            frameTransforms = shiftedTransforms,
                        ),
                        layers = state.layers.copy(layers = stack, frameLayers = shiftedLayers),
                        packTags = if (packTag == null) state.packTags else state.packTags + (target to packTag),
                    )
                }
                loadFrameInternal(target)
            }
        }
    }

    fun pasteFrame(index: Int, before: Boolean) {
        val clipboard = copiedFrame ?: return
        if (getState().animation.totalFrames >= 1000) return
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                if (currentState.animation.totalFrames >= 1000) return@withLock
                val destination = (if (before) index else index + 1).coerceIn(0, currentState.animation.totalFrames)
                settleOutgoingDrawing(currentState.animation.currentFrame)
                insertClipboardFrame(clipboard, destination)
            }
        }
    }

    fun copyFrame(index: Int) {
        val currentState = getState()
        val target = index.coerceIn(0, currentState.animation.totalFrames - 1)
        pendingOperations.launch(scope, dispatcher) {
            val layers = currentState.layers.frameLayers[target] ?: currentState.currentFrameLayers()
            val bitmaps = buildMap {
                layers.forEach { layer ->
                    cacheManager.getFrame(layer.id, target)?.let { bitmap ->
                        put(layer.id, bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true))
                    }
                }
            }
            copiedFrame = ClipboardFrame(layers, bitmaps)
            copiedFrames = listOf(copiedFrame!!)
        }
    }

    fun copyFrames(indices: List<Int>) {
        val currentState = getState()
        val targets = indices.distinct().filter { it in 0 until currentState.animation.totalFrames }.sorted()
        if (targets.isEmpty()) return
        pendingOperations.launch(scope, dispatcher) {
            copiedFrames = targets.mapNotNull { target ->
                val layers = currentState.layers.frameLayers[target] ?: currentState.currentFrameLayers()
                val bitmaps = buildMap {
                    layers.forEach { layer ->
                        cacheManager.getFrame(layer.id, target)?.let { bitmap ->
                            put(layer.id, bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true))
                        }
                    }
                }
                ClipboardFrame(layers, bitmaps)
            }
            copiedFrame = copiedFrames.firstOrNull()
        }
    }

    fun pasteFrames(index: Int, before: Boolean) {
        val frames = copiedFrames
        if (frames.isEmpty()) return
        frames.forEachIndexed { offset, _ ->
            pasteFrame(index + if (before) offset else offset, before)
        }
    }

    fun deleteFrames(indices: List<Int>) {
        if (indices.none { it in 0 until getState().animation.totalFrames }) return

        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                val targets = indices.distinct()
                    .filter { it in 0 until currentState.animation.totalFrames }
                    .sortedDescending()
                if (targets.isEmpty()) return@withLock
                settleOutgoingDrawing(currentState.animation.currentFrame)
                removeFrames(currentState, targets)
            }
        }
    }

    /**
     * Bakes the frame on the canvas into its own slot and drops its undo history. Must run before
     * frames shift: the next frame load re-bakes any history at the old index, which after a
     * delete or paste belongs to a different frame or to none (R3-18).
     */
    private suspend fun settleOutgoingDrawing(currentFrame: Int) {
        finalizeFrame(currentFrame)
        clearHistory()
    }

    /** Inserts [clipboard] as a new frame at [destination]. The caller holds the frame lock. */
    private suspend fun insertClipboardFrame(clipboard: ClipboardFrame, destination: Int) {
        val currentState = getState()
        val sourceLayers = clipboard.layers
        val cacheLayerIds = (currentState.currentFrameLayers() + sourceLayers).map { it.id }.distinct()
        cacheLayerIds.forEach { layerId ->
            cacheManager.insertFrame(layerId, destination, currentState.animation.totalFrames)
        }
        sourceLayers.forEach { layer ->
            clipboard.bitmaps[layer.id]?.let { bitmap ->
                cacheManager.putFrame(
                    layer.id,
                    destination,
                    bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                )
            }
        }
        commitFrameStructure(destination, emptyList()) { state ->
            val shiftedLayers = state.layers.frameLayers.mapKeys { (key, _) ->
                if (key >=
                    destination
                ) {
                    key + 1
                } else {
                    key
                }
            }.toMutableMap()
            val shiftedTransforms = state.animation.frameTransforms.mapKeys { (key, _) ->
                if (key >=
                    destination
                ) {
                    key + 1
                } else {
                    key
                }
            }.toMutableMap()
            shiftedLayers[destination] = sourceLayers
            state.copy(
                animation = state.animation.copy(
                    totalFrames = state.animation.totalFrames + 1,
                    currentFrame = destination,
                    frameTransforms = shiftedTransforms,
                ),
                layers = state.layers.copy(
                    activeLayerIndex = 0,
                    layers = sourceLayers,
                    frameLayers = shiftedLayers,
                ),
            )
        }
        loadFrameInternal(destination)
    }

    /** Removes [targets], highest first, from the cache and the state. The caller holds the frame lock. */
    private suspend fun removeFrames(currentState: StudioState, targets: List<Int>) {
        var currentTotalFrames = currentState.animation.totalFrames
        val currentActiveLayers = currentState.currentFrameLayers()

        for (index in targets) {
            currentActiveLayers.forEach { layer ->
                cacheManager.deleteFrame(layer.id, index, currentTotalFrames)
            }
            currentTotalFrames--
        }

        commitFrameStructure(null, targets) { state ->
            var newTotalFrames = state.animation.totalFrames
            var newFrameLayers = state.layers.frameLayers
            var newFrameTransforms = state.animation.frameTransforms
            var newCurrentFrame = state.animation.currentFrame

            for (index in targets) {
                newFrameLayers = newFrameLayers.entries
                    .filter { entry -> entry.key != index }
                    .associate { entry ->
                        val newIndex = if (entry.key > index) entry.key - 1 else entry.key
                        newIndex to entry.value
                    }

                newFrameTransforms = newFrameTransforms.entries
                    .filter { entry -> entry.key != index }
                    .associate { entry ->
                        val newIndex = if (entry.key > index) entry.key - 1 else entry.key
                        newIndex to entry.value
                    }

                if (newCurrentFrame > index) {
                    newCurrentFrame--
                }
                newTotalFrames--
            }

            newCurrentFrame = newCurrentFrame.coerceIn(0, maxOf(0, newTotalFrames - 1))

            state.copy(
                animation = state.animation.copy(
                    totalFrames = newTotalFrames,
                    currentFrame = newCurrentFrame,
                    frameTransforms = newFrameTransforms,
                ),
                layers = state.layers.copy(
                    frameLayers = newFrameLayers,
                    layers = newFrameLayers[newCurrentFrame] ?: state.layers.layers,
                ),
            )
        }
        loadFrameInternal(getState().animation.currentFrame)
    }

    /**
     * Moves one frame to [to], shifting the frames between (R3-20). Built from delete and insert,
     * so per-frame guides, pack tags and camera shots follow as they do for those.
     */
    fun moveFrame(from: Int, to: Int) {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                val total = currentState.animation.totalFrames
                if (from !in 0 until total || to !in 0 until total || from == to) return@withLock
                settleOutgoingDrawing(currentState.animation.currentFrame)
                val moving = captureFrame(currentState, from)
                removeFrames(currentState, listOf(from))
                insertClipboardFrame(moving, to)
            }
        }
    }

    private suspend fun captureFrame(state: StudioState, index: Int): ClipboardFrame {
        val layers = state.layers.frameLayers[index] ?: state.currentFrameLayers()
        val bitmaps = buildMap {
            layers.forEach { layer ->
                cacheManager.getFrame(layer.id, index)?.let { bitmap ->
                    put(layer.id, bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true))
                }
            }
        }
        return ClipboardFrame(layers, bitmaps)
    }

    fun deleteFrame(index: Int) {
        deleteFrames(listOf(index))
    }

    fun duplicateFrame(index: Int) {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                if (currentState.animation.totalFrames >= 1000) {
                    sendEffect(StudioEffect.ShowError(StudioFailure.FRAME_LIMIT))
                    return@withLock
                }
                if (index !in 0 until currentState.animation.totalFrames) return@withLock
                finalizeFrame(currentState.animation.currentFrame)
                clearHistory()
                currentState.currentFrameLayers().forEach { layer ->
                    cacheManager.duplicateFrame(layer.id, index, currentState.animation.totalFrames)
                }

                val newTotalFrames = currentState.animation.totalFrames + 1
                val newCurrentFrame = index + 1

                commitFrameStructure(newCurrentFrame, emptyList()) { state ->
                    val sourceLayers = state.layers.frameLayers[index] ?: state.currentFrameLayers()
                    val sourceTransform = state.animation.frameTransforms[index]

                    val shiftedLayers = state.layers.frameLayers.mapKeys { (key, _) ->
                        if (key > index) key + 1 else key
                    }.toMutableMap()
                    val shiftedTransforms = state.animation.frameTransforms.mapKeys { (key, _) ->
                        if (key >
                            index
                        ) {
                            key + 1
                        } else {
                            key
                        }
                    }.toMutableMap()

                    shiftedLayers[index + 1] = sourceLayers
                    sourceTransform?.let { shiftedTransforms[index + 1] = it }

                    state.copy(
                        animation = state.animation.copy(
                            totalFrames = newTotalFrames,
                            currentFrame = newCurrentFrame,
                            frameTransforms = shiftedTransforms,
                        ),
                        layers = state.layers.copy(
                            layers = shiftedLayers[newCurrentFrame] ?: sourceLayers,
                            frameLayers = shiftedLayers,
                        ),
                    )
                }
                loadFrameInternal(newCurrentFrame)
            }
        }
    }

    fun handle(intent: StudioIntent.Frame) {
        when (intent) {
            StudioIntent.Frame.AddFrame -> addFrame()

            is StudioIntent.Frame.DeleteFrame -> deleteFrame(intent.index)

            is StudioIntent.Frame.DuplicateFrame -> duplicateFrame(intent.index)

            is StudioIntent.Frame.CopyFrame -> copyFrame(intent.index)

            is StudioIntent.Frame.CopyFrames -> copyFrames(intent.indices)

            is StudioIntent.Frame.PasteFrame -> pasteFrame(intent.index, intent.before)

            is StudioIntent.Frame.PasteFrames -> pasteFrames(intent.index, intent.before)

            is StudioIntent.Frame.DeleteFrames -> deleteFrames(intent.indices)

            is StudioIntent.Frame.AddFrameAt -> addFrameAt(intent.index)

            is StudioIntent.Frame.MoveFrame -> moveFrame(intent.from, intent.to)

            // Held drawings belong to CelHoldManager.
            is StudioIntent.Frame.HoldDrawing,
            is StudioIntent.Frame.EditDrawingScope,
            is StudioIntent.Frame.UnlinkDrawing,
            -> Unit
        }
    }
}
