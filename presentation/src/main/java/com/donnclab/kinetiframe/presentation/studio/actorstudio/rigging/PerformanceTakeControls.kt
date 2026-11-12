package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.actors.rigging.ActorToolButton
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
            ActorToolButton(Icons.Default.FiberManualRecord, stringResource(R.string.take_record)) {
                onIntent(StudioIntent.ActorMotion.StartTake)
            }
        } else {
            Text(
                when (phase) {
                    PerformanceTakePhase.ARMED -> stringResource(
                        if (state.actor.takeMoveCharacter) {
                            R.string.take_armed_move_handle
                        } else {
                            R.string.take_armed_move_part
                        },
                    )

                    PerformanceTakePhase.RECORDING -> stringResource(
                        R.string.take_recording_seconds,
                        (state.actor.previewTimeMicros - state.actor.takeStartMicros) / 1_000_000f,
                    )

                    PerformanceTakePhase.REVIEW -> stringResource(R.string.take_review)

                    PerformanceTakePhase.IDLE -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 6.dp),
            )
            if (phase == PerformanceTakePhase.REVIEW) {
                ActorToolButton(
                    if (state.actor.previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    stringResource(R.string.take_replay),
                ) {
                    onIntent(StudioIntent.ActorMotion.TogglePlayback)
                }
                var expanded by remember { mutableStateOf(false) }
                Box {
                    ActorToolButton(Icons.Default.Tune, stringResource(R.string.take_smoothness)) { expanded = true }
                    DropdownMenu(expanded, { expanded = false }) {
                        listOf(
                            0f to R.string.take_smoothness_faithful,
                            .35f to R.string.take_smoothness_gentle,
                            .75f to R.string.take_smoothness_smooth,
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label), style = MaterialTheme.typography.labelSmall) },
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
                ActorToolButton(Icons.Default.Check, stringResource(R.string.take_keep)) {
                    onIntent(StudioIntent.ActorMotion.KeepTake)
                }
                ActorToolButton(Icons.Default.Replay, stringResource(R.string.take_retake)) {
                    onIntent(StudioIntent.ActorMotion.Retake)
                }
            } else {
                ActorToolButton(
                    Icons.Default.OpenWith,
                    stringResource(R.string.take_move_character),
                    selected = state.actor.takeMoveCharacter,
                ) {
                    onIntent(StudioIntent.ActorMotion.SetTakeMoveCharacter(!state.actor.takeMoveCharacter))
                }
                ActorToolButton(Icons.Default.Stop, stringResource(R.string.take_stop)) {
                    onIntent(StudioIntent.ActorMotion.StopTake)
                }
            }
            ActorToolButton(Icons.Default.Close, stringResource(R.string.take_discard)) {
                onIntent(StudioIntent.ActorMotion.CancelTake)
            }
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
