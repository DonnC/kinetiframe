package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.ActorStudioState
import com.donnclab.kinetiframe.studio.PerformanceTakePhase
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/** Record first, timeline second. Only review exposes take processing and confirmation. */
@Composable
fun PerformanceTakeControls(state: StudioState, onIntent: (StudioIntent) -> Unit) {
    val phase = state.actor.takePhase
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (phase == PerformanceTakePhase.IDLE) {
            ActorToolButton(Icons.Default.FiberManualRecord, "Record movement") {
                onIntent(StudioIntent.ActorMotion.StartTake)
            }
        } else {
            Text(
                when (phase) {
                    PerformanceTakePhase.ARMED ->
                        if (state.actor.takeMoveCharacter) "Drag the move handle to start" else "Move a part to start"

                    PerformanceTakePhase.RECORDING -> "Recording · ${"%.1f".format(
                        (state.actor.previewTimeMicros - state.actor.takeStartMicros) / 1_000_000f,
                    )}s / 30s"

                    PerformanceTakePhase.REVIEW -> "Review take"

                    PerformanceTakePhase.IDLE -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 6.dp),
            )
            if (phase == PerformanceTakePhase.REVIEW) {
                ActorToolButton(
                    if (state.actor.previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Replay take",
                ) {
                    onIntent(StudioIntent.ActorMotion.TogglePlayback)
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    ActorToolButton(Icons.Default.Tune, "Recording smoothness") { expanded = true }
                    DropdownMenu(expanded, { expanded = false }) {
                        listOf(0f to "Faithful", .35f to "Gentle", .75f to "Smooth").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = {
                                    if (state.actor.takeSmoothness ==
                                        value
                                    ) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onIntent(StudioIntent.ActorMotion.SetTakeSmoothness(value))
                                },
                            )
                        }
                    }
                }
                ActorToolButton(Icons.Default.Check, "Keep take") { onIntent(StudioIntent.ActorMotion.KeepTake) }
                ActorToolButton(Icons.Default.Replay, "Retake movement") { onIntent(StudioIntent.ActorMotion.Retake) }
            } else {
                ActorToolButton(
                    Icons.Default.OpenWith,
                    "Move whole character",
                    selected = state.actor.takeMoveCharacter,
                ) {
                    onIntent(StudioIntent.ActorMotion.SetTakeMoveCharacter(!state.actor.takeMoveCharacter))
                }
                ActorToolButton(Icons.Default.Stop, "Stop recording") { onIntent(StudioIntent.ActorMotion.StopTake) }
            }
            ActorToolButton(Icons.Default.Close, "Discard take") { onIntent(StudioIntent.ActorMotion.CancelTake) }
        }
    }
}

@Preview
@Composable
private fun TakeControlsPreview() {
    MaterialTheme {
        PerformanceTakeControls(StudioState(actor = ActorStudioState(takePhase = PerformanceTakePhase.REVIEW)), {})
    }
}
