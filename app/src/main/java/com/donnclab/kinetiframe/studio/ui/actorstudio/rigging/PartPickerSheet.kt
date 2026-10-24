package com.donnclab.kinetiframe.studio.ui.actorstudio.rigging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.donnclab.kinetiframe.actors.ui.rigging.ActorPartTree
import com.donnclab.kinetiframe.actors.ui.rigging.CoverageBadge
import com.donnclab.kinetiframe.actors.ui.rigging.RasterLayerGroupHeader
import com.donnclab.kinetiframe.actors.ui.rigging.RasterLayerRow
import com.donnclab.kinetiframe.core.document.LayerType
import com.donnclab.kinetiframe.core.document.PuppetDefinition
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers

/**
 * The full part hierarchy, one tap away from [PartRail]: actor name, artwork coverage, the raster
 * layer list, and the complete bone tree with visibility toggles. It opens on demand from the Rig
 * Fabricator top bar's part-list icon so it doesn't take canvas space permanently.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartPickerSheet(
    state: StudioState,
    definition: PuppetDefinition,
    onIntent: (StudioIntent) -> Unit,
    onDismiss: () -> Unit,
    onReferenceClick: (() -> Unit)? = null,
) {
    val layers = state.currentFrameLayers()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ActorStudioTokens.panelSurface) {
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Parts",
                        color = ActorStudioTokens.onPanel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onIntent(StudioIntent.Layer.AddLayer(LayerType.RASTER)) }) {
                        Icon(
                            Lucide.Plus,
                            "Add layer",
                            tint = ActorStudioTokens.onPanel,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    ActorNameRow(definition.name, onIntent)
                    CoverageBadge(definition)
                }
            }
            if (onReferenceClick != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Reference · temporary", modifier = Modifier.weight(1f), color = ActorStudioTokens.onPanel)
                        IconButton(onClick = onReferenceClick) {
                            Icon(Lucide.Image, "Manage temporary reference", tint = ActorStudioTokens.onPanel)
                        }
                    }
                }
            }
            item { RasterLayerGroupHeader() }
            items(layers.filter { it.type == LayerType.RASTER }, key = { "raster-${it.id}" }) { layer ->
                val actualIndex = layers.indexOf(layer)
                RasterLayerRow(
                    name = layer.name,
                    isActive = actualIndex == state.layers.activeLayerIndex,
                    isVisible = layer.isVisible,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = { onIntent(StudioIntent.Layer.SelectLayer(actualIndex)) },
                    onVisibilityClick = {
                        onIntent(StudioIntent.Layer.SetLayerVisibility(actualIndex, !layer.isVisible))
                    },
                )
            }
            item {
                ActorPartTree(
                    graph = definition.sceneGraph,
                    selectedPartId = state.scene.selectedBoneId,
                    onPartSelected = { onIntent(StudioIntent.Rigging.SelectBone(it)) },
                    onVisibilityToggle = { boneId, visible ->
                        onIntent(StudioIntent.Actor.SetPartVisibility(boneId, visible))
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
    }
}
