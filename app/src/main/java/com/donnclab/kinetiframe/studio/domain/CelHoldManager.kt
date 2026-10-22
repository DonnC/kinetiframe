package com.donnclab.kinetiframe.studio.domain

import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Raster exposure authoring only; frame indices, scene objects and actor rigs remain separate. */
class CelHoldManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val cache: FrameCacheManager,
    private val frameMutex: Mutex,
    private val getState: () -> StudioState,
    private val updateState: ((StudioState) -> StudioState) -> Unit,
    private val sendEffect: (StudioEffect) -> Unit,
    private val finalizeDrawing: suspend () -> Unit,
    private val clearHistory: () -> Unit,
    private val loadFrame: suspend (Int) -> Unit,
    private val pending: PendingOperations,
) {
    fun hold(first: Int, last: Int) = operate(first) { layer, total -> cache.holdDrawing(layer, first, last, total) }
    fun unlink(frame: Int) = operate(frame) { layer, _ -> cache.detachDrawing(layer, frame) }
    fun editScope(frame: Int, allLinked: Boolean) = operate(frame, allLinked) { layer, _ ->
        if (!allLinked) cache.detachDrawing(layer, frame)
    }

    private fun operate(frame: Int, allLinked: Boolean = false, action: suspend (String, Int) -> Unit) {
        val requested = getState().let { it.currentFrameLayers().getOrNull(it.layers.activeLayerIndex)?.id }
        pending.launch(scope, dispatcher) {
            frameMutex.withLock {
                try {
                    val state = getState()
                    require(frame in 0 until state.animation.totalFrames) { "Frame is unavailable" }
                    require(state.currentFrameLayers().getOrNull(state.layers.activeLayerIndex)?.id == requested) {
                        "The active layer changed; choose the drawing again"
                    }
                    val layer =
                        requireNotNull(
                            state.currentFrameLayers().firstOrNull {
                                it.id == requested
                            },
                        ) { "Layer is unavailable" }
                    require(layer.type == LayerType.RASTER && !layer.isLocked) { "Choose an unlocked drawing layer" }
                    finalizeDrawing()
                    action(layer.id, state.animation.totalFrames)
                    clearHistory()
                    updateState {
                        it.copy(
                            animation = it.animation.copy(currentFrame = frame),
                            layers = it.layers.copy(linkedCels = cache.exposures(), linkedEditTarget = null),
                        )
                    }
                    loadFrame(frame)
                    // loadFrame resets scope. Set opt-in only after loading its independent base.
                    cache.editLinked(layer.id, frame, allLinked)
                    updateState {
                        it.copy(layers = it.layers.copy(linkedEditTarget = if (allLinked) layer.id to frame else null))
                    }
                } catch (
                    cancel: kotlinx.coroutines.CancellationException,
                ) {
                    throw cancel
                } catch (error: Exception) {
                    sendEffect(StudioEffect.ShowError(error.message ?: "Could not update held drawing"))
                }
            }
        }
    }
}
