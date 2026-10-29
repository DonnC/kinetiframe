package com.donnclab.kinetiframe.presentation.studio.actorstudio.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.activeActor

/** Timing is a saved authoring choice, not a cosmetic filter on the preview. */
@Composable
fun ActorMotionTimingMenu(state: StudioState, onIntent: (StudioIntent) -> Unit, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(Icons.Default.Speed, "Motion timing")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                CurveInterpolation.SMOOTH to "Flow · continuous",
                CurveInterpolation.EASE_IN_OUT to "Ease · pose to pose",
                CurveInterpolation.LINEAR to "Linear",
                CurveInterpolation.STEP to "Hold",
            ).forEach { (mode, label) ->
                DropdownMenuItem(text = { Text(label) }, trailingIcon = {
                    if (state.activeActor()?.poseInterpolation == mode) Text("✓")
                }, onClick = {
                    expanded = false
                    onIntent(StudioIntent.ActorMotion.SetInterpolation(mode))
                })
            }
        }
    }
}

@Preview
@Composable
private fun MotionTimingPreview() = ActorMotionTimingMenu(StudioState(), {}, true)
