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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.actors.rig.presets.ActorPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R

/** Preset auditioning strip plus the Apply button. One of the sections composed by [PuppetInspector]. */
@Composable
fun PresetLibrarySection(
    presets: List<ActorPreset>,
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
                stringResource(R.string.preset_library_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ActorStudioTokens.onPanel,
                modifier = Modifier.weight(1f),
            )
        }

        if (!inPerformanceMode) {
            Text(
                stringResource(R.string.preset_library_switch_mode),
                style = MaterialTheme.typography.labelSmall,
                color = ActorStudioTokens.onPanelMuted,
                modifier = Modifier.padding(top = Dimens.spaceXs),
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            modifier = Modifier.padding(top = Dimens.spaceS),
        ) {
            if (presets.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.preset_library_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = ActorStudioTokens.onPanelMuted,
                    )
                }
            } else {
                items(presets, key = { it.recipe.id }) { preset ->
                    ActorPresetCard(
                        preset = preset,
                        selected = previewingId == preset.recipe.id,
                        enabled = inPerformanceMode,
                        onClick = { onPreview(preset.recipe.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.spaceM))

        // Icon action, not a full-width button -- Preset Library lives in a small sheet now, and
        // a single confirm affordance beside the row it applies to reads clearer here than a bar
        // spanning the whole sheet width.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.preset_library_apply_hint),
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
                    stringResource(R.string.preset_library_apply),
                    tint = if (canApply &&
                        inPerformanceMode
                    ) {
                        ActorStudioTokens.onAccent
                    } else {
                        ActorStudioTokens.onPanelMuted
                    },
                )
            }
        }
    }
}
