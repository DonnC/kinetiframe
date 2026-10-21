package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.model.PuppetDefinition
import com.donnclab.kinetiframe.engine.model.PuppetInstance
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.models.LayerType
import com.donnclab.kinetiframe.ui.LayerState
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.currentFrameLayers

/**
 * Manager for layer operations.
 */
class LayerManager(
    private val updateState: ((TimelineState) -> TimelineState) -> Unit,
    private val refreshComposite: () -> Unit,
    private val onLayerAdded: (String) -> Unit = {},
) {
    fun addLayer(type: LayerType, boundBoneId: String? = null, customName: String? = null): String? {
        var newLayerId: String? = null
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            val nextIndex = currentLayers.size
            val visibleCount = currentLayers.count { it.id != "main" }
            val layerId = "layer_${nextIndex}_${System.nanoTime()}"
            val puppetDefinition = if (type == LayerType.PUPPET) {
                PuppetDefinition(id = "puppet_${System.nanoTime()}", name = "Puppet ${visibleCount + 1}")
            } else {
                null
            }
            val puppetInstance = puppetDefinition?.let { definition ->
                PuppetInstance(
                    id = "instance_${System.nanoTime()}",
                    definitionId = definition.id,
                )
            }
            val layer = Layer(
                name = customName ?: when (type) {
                    LayerType.BACKGROUND -> "Background"
                    LayerType.PUPPET -> "Puppet ${visibleCount + 1}"
                    LayerType.RASTER -> "Layer ${visibleCount + 1}"
                },
                id = layerId,
                type = type,
                zIndex = nextIndex,
                puppetInstanceId = puppetInstance?.id,
                boundBoneId = boundBoneId,
            )
            newLayerId = layer.id
            val updated = currentLayers + layer
            state.copy(
                project = state.project.copy(
                    layers = updated,
                    puppetDefinitions = puppetDefinition?.let { state.project.puppetDefinitions + (it.id to it) }
                        ?: state.project.puppetDefinitions,
                    puppetInstances = puppetInstance?.let { state.project.puppetInstances + (it.id to it) }
                        ?: state.project.puppetInstances,
                ),
                layers = state.layers.withStack(updated, nextIndex),
            )
        }
        newLayerId?.let { onLayerAdded(it) }
        refreshComposite()
        return newLayerId
    }

    fun removeLayer(index: Int) {
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (currentLayers.size <= 1 || index !in currentLayers.indices) {
                state
            } else {
                val remaining = currentLayers.filterIndexed { i, _ -> i != index }
                    .mapIndexed { i, layer -> layer.copy(zIndex = i) }
                state.copy(
                    project = state.project.copy(layers = remaining),
                    // withStack clamps the active index into the surviving stack for us.
                    layers = state.layers.withStack(remaining),
                )
            }
        }
        refreshComposite()
    }

    fun reorderLayer(fromIndex: Int, toIndex: Int) {
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (fromIndex !in currentLayers.indices || toIndex !in currentLayers.indices) {
                state
            } else {
                val reordered = currentLayers.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                    .mapIndexed { i, layer -> layer.copy(zIndex = i) }
                val newActiveIndex = when {
                    state.layers.activeLayerIndex == fromIndex -> toIndex

                    fromIndex < state.layers.activeLayerIndex && toIndex >= state.layers.activeLayerIndex ->
                        state.layers.activeLayerIndex -
                            1

                    fromIndex > state.layers.activeLayerIndex && toIndex <= state.layers.activeLayerIndex ->
                        state.layers.activeLayerIndex +
                            1

                    else -> state.layers.activeLayerIndex
                }
                state.copy(
                    project = state.project.copy(layers = reordered),
                    layers = state.layers.withStack(reordered, newActiveIndex),
                )
            }
        }
        refreshComposite()
    }

    fun setLayerVisibility(index: Int, visible: Boolean) {
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (index !in currentLayers.indices) {
                state
            } else {
                val updated = currentLayers.mapIndexed { i, layer ->
                    if (i == index) layer.copy(isVisible = visible) else layer
                }
                state.copy(
                    project = state.project.copy(layers = updated),
                    layers = state.layers.withStack(updated),
                )
            }
        }
        refreshComposite()
    }

    fun toggleLayerLock(index: Int) {
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (index !in currentLayers.indices) {
                state
            } else {
                val updated = currentLayers.mapIndexed { i, layer ->
                    if (i == index) layer.copy(isLocked = !layer.isLocked) else layer
                }
                state.copy(
                    project = state.project.copy(layers = updated),
                    layers = state.layers.withStack(updated),
                )
            }
        }
        refreshComposite()
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (index !in currentLayers.indices) {
                state
            } else {
                val updated = currentLayers.mapIndexed { i, layer ->
                    if (i == index) layer.copy(opacity = opacity.coerceIn(0f, 1f)) else layer
                }
                state.copy(
                    project = state.project.copy(layers = updated),
                    layers = state.layers.withStack(updated),
                )
            }
        }
        refreshComposite()
    }

    fun renameLayer(index: Int, name: String) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return
        updateState { state ->
            val currentLayers = state.currentFrameLayers()
            if (index !in currentLayers.indices) {
                state
            } else {
                val updated = currentLayers.mapIndexed { i, layer ->
                    if (i == index) layer.copy(name = cleaned) else layer
                }
                state.copy(
                    project = state.project.copy(layers = updated),
                    layers = state.layers.withStack(updated),
                )
            }
        }
    }
}

/**
 * Applies a new layer stack to every place that holds one, keeping them from drifting apart.
 *
 * A layer stack is project-wide (see [com.donnclab.kinetiframe.ui.layersForFrame]), so a mutation must land
 * on the document, on the live [LayerState], and on every frame's legacy entry at once. Updating
 * only `frameLayers[currentFrame]` would leave every other frame holding a stale stack.
 */
private fun LayerState.withStack(updated: List<Layer>, activeIndex: Int = activeLayerIndex) = copy(
    layers = updated,
    frameLayers = frameLayers.keys.associateWith { updated },
    activeLayerIndex = activeIndex.coerceIn(0, maxOf(0, updated.lastIndex)),
)
