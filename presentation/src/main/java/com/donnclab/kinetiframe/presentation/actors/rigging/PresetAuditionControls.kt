package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.valueText

/**
 * The preset being auditioned and what its controls do. [replacesKeys] warns that applying it
 * replaces the performance's keys; a null [onRemove] hides the remove button.
 */
data class PresetAudition(
    val preview: AppliedActorPreset?,
    val playing: Boolean,
    val replacesKeys: Boolean,
    val onPlay: () -> Unit,
    val onIntensity: (Float) -> Unit,
    val onSpeed: (Float) -> Unit,
    val onRemove: (() -> Unit)?,
)

/** All audition controls stay in the dark sheet; no stacked dialog obscures the actor. */
@Composable
fun PresetAuditionControls(audition: PresetAudition, modifier: Modifier = Modifier) {
    val (preview, playing, replacesKeys, onPlay, onIntensity, onSpeed, onRemove) = audition
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlay, enabled = preview != null) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    stringResource(R.string.preset_preview_motion),
                    tint = ActorStudioTokens.accent,
                )
            }
            Text(
                preview?.recipe?.name ?: stringResource(R.string.preset_select_motion),
                style = MaterialTheme.typography.labelMedium,
                color = ActorStudioTokens.onPanel,
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        stringResource(R.string.preset_remove_motion),
                        tint = ActorStudioTokens.onPanelMuted,
                    )
                }
            }
        }
        if (preview != null) {
            PresetDial(stringResource(R.string.preset_strength), preview.intensity, 0f..2f, onIntensity)
            PresetDial(stringResource(R.string.preset_speed), preview.speed, .25f..3f, onSpeed)
            if (replacesKeys) {
                Text(
                    stringResource(R.string.preset_replaces_keys),
                    style = MaterialTheme.typography.labelSmall,
                    color = ActorStudioTokens.onPanelMuted,
                )
            }
        }
    }
}

@Composable
private fun PresetDial(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    // One clock restart on release, not a job cancel/relaunch for every slider sample.
    com.donnclab.kinetiframe.presentation.common.StandardSlider(
        draft, { draft = it }, range, label,
        valueDisplay = valueText(R.string.value_multiplier) { it },
        accentColor = ActorStudioTokens.accent, labelColor = ActorStudioTokens.onPanelMuted,
        valueColor = ActorStudioTokens.onPanel, onValueChangeFinished = { onValue(draft) },
    )
}

@Preview
@Composable
private fun AuditionControlsPreview() = PresetAuditionControls(PresetAudition(null, false, false, {}, {}, {}, null))
