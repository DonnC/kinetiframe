package com.donnclab.kinetiframe.studio.ui.timeline

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.LayerState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.render.FrameCacheManager

@Composable
fun LayerPanel(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    onLayerClick: (Int) -> Unit = {},
    cacheManager: FrameCacheManager? = null,
) {
    val allLayers = state.currentFrameLayers()
    // Presented top-most-first, the way every layer panel reads (FlipaClip, Procreate, Photoshop).
    // The compositor draws ascending zIndex, so index 0 is the BACK-most layer -- listing the raw
    // order put the backmost layer at the top of the panel and made dragging a row DOWN bring it
    // to the FRONT, the opposite of what the row's position implied. Only the presentation is
    // reversed: every action below maps its row back to the real stack by layer id
    // (`allLayers.indexOfFirst { it.id == ... }`), so the reorder maths needs no other change.
    // The main raster layer is editable artwork too. Hiding it prevented animators from
    // moving its drawings in front of other layers or even seeing which layer was selected.
    val layers = allLayers.reversed()
    var editingLayerId by remember { mutableStateOf<String?>(null) }
    var addLayerMenuVisible by remember { mutableStateOf(false) }
    val previews = remember { mutableStateMapOf<String, Bitmap>() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.animation.currentFrame, layers, cacheManager) {
        previews.clear()
        if (cacheManager != null) {
            layers.forEach { layer ->
                cacheManager.getFrame(layer.id, state.animation.currentFrame)?.let { previews[layer.id] = it }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Lucide.Layers,
                contentDescription = null,
                tint = EditorTokens.accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Layers",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = { addLayerMenuVisible = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Lucide.Plus,
                        contentDescription = "Add Layer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = addLayerMenuVisible, onDismissRequest = { addLayerMenuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text("Drawing layer") },
                        onClick = {
                            onIntent(StudioIntent.Layer.AddLayer())
                            addLayerMenuVisible = false
                        },
                        leadingIcon = { Icon(Lucide.Layers, null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (layers.isEmpty()) {
                item {
                    Text(
                        text = "No layers",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorTokens.muted,
                    )
                }
            } else {
                itemsIndexed(
                    items = layers,
                    key = { _, layer -> layer.id },
                ) { index, layer ->
                    val actualIndex = allLayers.indexOfFirst { it.id == layer.id }
                    val isActive = actualIndex == state.layers.activeLayerIndex

                    LayerItem(
                        layer = layer,
                        isActive = isActive,
                        preview = previews[layer.id],
                        editing = editingLayerId == layer.id,
                        onEdit = { editingLayerId = layer.id },
                        onRename = { name: String ->
                            onIntent(StudioIntent.Layer.RenameLayer(actualIndex, name))
                            editingLayerId = null
                        },
                        dragHandleModifier = Modifier
                            .pointerInput(index, layers) {
                                var distance = 0f
                                detectDragGestures(
                                    onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        distance += amount.y
                                        if (kotlin.math.abs(distance) > 40f) {
                                            val targetVisible = (
                                                index + if (distance >
                                                    0
                                                ) {
                                                    1
                                                } else {
                                                    -1
                                                }
                                                ).coerceIn(layers.indices)
                                            val target = allLayers.indexOfFirst { it.id == layers[targetVisible].id }
                                            if (target != actualIndex) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onIntent(StudioIntent.Layer.ReorderLayer(actualIndex, target))
                                            }
                                            distance = 0f
                                        }
                                    },
                                )
                            },
                        modifier = Modifier.animateItem(),
                        onClick = {
                            onIntent(StudioIntent.Layer.SelectLayer(actualIndex))
                        },
                        onVisibilityClick = {
                            onIntent(StudioIntent.Layer.SetLayerVisibility(actualIndex, !layer.isVisible))
                        },
                        onOpacityChange = { value: Float ->
                            onIntent(StudioIntent.Layer.SetLayerOpacity(actualIndex, value))
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LayerPanelPreview() {
    KinetiFrameTheme {
        LayerPanel(
            state = StudioState(
                layers = LayerState(
                    layers = listOf(
                        Layer(name = "Layer 1"),
                        Layer(name = "Layer 2"),
                        Layer(name = "Layer 3"),
                    ),
                    activeLayerIndex = 1,
                ),
            ),
            onIntent = {},
        )
    }
}
