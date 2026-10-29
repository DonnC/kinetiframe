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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview

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
    val locale = LocalConfiguration.current.locales[0]
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(String.format(locale, "%.2f s", durationMicros / 1_000_000.0))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(500_000L, 1_000_000L, 2_000_000L, 3_000_000L, 5_000_000L, 10_000_000L).forEach { duration ->
                DropdownMenuItem(
                    text = { Text(String.format(locale, "%.1f seconds", duration / 1_000_000.0)) },
                    enabled = duration in minimumMicros..maximumMicros,
                    onClick = {
                        onDuration(duration)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Shorten by one frame") },
                enabled = durationMicros - frameMicros >= minimumMicros,
                onClick = {
                    onDuration(durationMicros - frameMicros)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Extend by one frame") },
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
