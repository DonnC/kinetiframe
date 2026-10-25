package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/** Preset auditioning strip plus the Apply button. One of the sections composed by [PuppetInspector]. */
@Composable
fun PresetLibrarySection(
    presets: List<ActorMotionPreset>,
    previewingId: String?,
    inPerformanceMode: Boolean,
    onPreview: (String) -> Unit,
    onApply: () -> Unit,
    canApply: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Preset Library",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ActorStudioTokens.onPanel,
                modifier = Modifier.weight(1f),
            )
        }

        if (!inPerformanceMode) {
            Text(
                "Switch to Animate mode to preview and apply presets.",
                style = MaterialTheme.typography.labelSmall,
                color = ActorStudioTokens.onPanelMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (presets.isEmpty()) {
                item {
                    Text(
                        "No presets available",
                        style = MaterialTheme.typography.labelSmall,
                        color = ActorStudioTokens.onPanelMuted,
                    )
                }
            } else {
                items(presets, key = { it.id }) { preset ->
                    ActorPresetCard(
                        preset = preset,
                        selected = previewingId == preset.id,
                        enabled = inPerformanceMode,
                        onClick = { onPreview(preset.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Icon action, not a full-width button -- Preset Library lives in a small sheet now, and
        // a single confirm affordance beside the row it applies to reads clearer here than a bar
        // spanning the whole sheet width.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Apply to this actor's rig",
                style = MaterialTheme.typography.labelSmall,
                color = ActorStudioTokens.onPanelMuted,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onApply,
                enabled = canApply && inPerformanceMode,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ActorStudioTokens.accent,
                    disabledContainerColor = ActorStudioTokens.panelSurfaceVariant,
                ),
            ) {
                Icon(
                    Lucide.Check,
                    "Apply retargeting",
                    tint = if (canApply && inPerformanceMode) Color(0xFF0B1416) else ActorStudioTokens.onPanelMuted,
                )
            }
        }
    }
}
