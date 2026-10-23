package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.studio.StudioEffect
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
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val cacheManager: FrameCacheManager,
    private val frameOperationMutex: Mutex,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
    private val loadFrameInternal: suspend (Int) -> Unit,
    private val finalizeFrame: suspend (Int) -> Unit,
    private val clearHistory: () -> Unit,
    /** UI-only guides must be remapped in the same publication turn as the frame indices. */
    private val commitFrameStructure: suspend (Int?, List<Int>, (StudioState) -> StudioState) -> Unit =
        { _, _, change -> updateState(change) },
    private val pendingOperations: PendingOperations = PendingOperations(),
) {
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
                    sendEffect(StudioEffect.ShowError("Maximum frame limit reached"))
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

    fun addFrameAt(index: Int, packTag: com.donnclab.kinetiframe.library.ui.packs.PackFrameTag? = null) {
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
        val currentState = getState()
        if (currentState.animation.totalFrames >= 1000) return
        val destination = (if (before) index else index + 1).coerceIn(0, currentState.animation.totalFrames)
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
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
        val currentState = getState()
        val targets = indices.distinct().filter { it in 0 until currentState.animation.totalFrames }.sortedDescending()
        if (targets.isEmpty()) return

        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
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
        }
    }

    fun deleteFrame(index: Int) {
        deleteFrames(listOf(index))
    }

    fun duplicateFrame(index: Int) {
        pendingOperations.launch(scope, dispatcher) {
            frameOperationMutex.withLock {
                val currentState = getState()
                if (currentState.animation.totalFrames >= 1000) {
                    sendEffect(StudioEffect.ShowError("Maximum frame limit reached"))
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

            // Held drawings belong to CelHoldManager.
            is StudioIntent.Frame.HoldDrawing,
            is StudioIntent.Frame.EditDrawingScope,
            is StudioIntent.Frame.UnlinkDrawing,
            -> Unit
        }
    }
}
