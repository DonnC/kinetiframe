package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.StudioIntent

/** Playback configuration is contextual, not a second permanent timeline row. */
@Composable
fun TimelineSpeedMenu(fps: Int, onIntent: (StudioIntent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.size(48.dp).semantics {
                contentDescription =
                    "Playback speed"
            },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("$fps", style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                "Playback FPS",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TimelineFpsSlider(fps, onIntent, Modifier.width(260.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineSpeedMenuPreview() {
    TimelineSpeedMenu(24, {})
}
