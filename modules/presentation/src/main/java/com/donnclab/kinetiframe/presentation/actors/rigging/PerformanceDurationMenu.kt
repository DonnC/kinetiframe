package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.presentation.R

/** Contextual timing choices keep the stage visible and avoid dozens of frame-step taps. */
@Composable
fun PerformanceDurationMenu(
    durationMicros: Long,
    minimumMicros: Long,
    maximumMicros: Long,
    frameMicros: Long,
    onDuration: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.performance_duration, durationMicros / 1_000_000.0))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(500_000L, 1_000_000L, 2_000_000L, 3_000_000L, 5_000_000L, 10_000_000L).forEach { duration ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.performance_duration_choice, duration / 1_000_000.0)) },
                    enabled = duration in minimumMicros..maximumMicros,
                    onClick = {
                        onDuration(duration)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.performance_shorten_frame)) },
                enabled = durationMicros - frameMicros >= minimumMicros,
                onClick = {
                    onDuration(durationMicros - frameMicros)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.performance_extend_frame)) },
                enabled = durationMicros + frameMicros <= maximumMicros,
                onClick = {
                    onDuration(durationMicros + frameMicros)
                    expanded = false
                },
            )
        }
    }
}

@Preview
@Composable
private fun PerformanceDurationMenuPreview() {
    PerformanceDurationMenu(2_000_000, 83_333, 30_000_000, 83_333, {})
}
