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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/** All audition controls stay in the dark sheet; no stacked dialog obscures the actor. */
@Composable
fun PresetAuditionControls(
    preview: AppliedActorPreset?,
    playing: Boolean,
    replacesKeys: Boolean,
    onPlay: () -> Unit,
    onIntensity: (Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlay, enabled = preview != null) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Preview motion",
                    tint = ActorStudioTokens.accent,
                )
            }
            Text(
                preview?.recipe?.name ?: "Select a motion",
                style = MaterialTheme.typography.labelMedium,
                color = ActorStudioTokens.onPanel,
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, "Remove applied motion", tint = ActorStudioTokens.onPanelMuted)
                }
            }
        }
        if (preview != null) {
            PresetDial("Strength", preview.intensity, 0f..2f, onIntensity)
            PresetDial("Speed", preview.speed, .25f..3f, onSpeed)
            if (replacesKeys) {
                Text(
                    "Apply replaces this performance's keys · undo available",
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
        valueDisplay = { String.format(java.util.Locale.ROOT, "%.2f×", it) },
        accentColor = ActorStudioTokens.accent, labelColor = ActorStudioTokens.onPanelMuted,
        valueColor = ActorStudioTokens.onPanel, onValueChangeFinished = { onValue(draft) },
    )
}

@Preview
@Composable
private fun AuditionControlsPreview() = PresetAuditionControls(null, false, false, {}, {}, {}, null)
