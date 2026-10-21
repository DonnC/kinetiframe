package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.donnclab.kinetiframe.ui.TimelineIntent

/** Recovery stays reachable even when every drawing toolbar is hidden. */
@Composable
fun StudioFocusControls(
    isPlaying: Boolean,
    onIntent: (TimelineIntent) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
    ) {
        Row {
            IconButton(onClick = {
                onIntent(if (isPlaying) TimelineIntent.Playback.Pause else TimelineIntent.Playback.Play)
            }) { Icon(if (isPlaying) Lucide.Pause else Lucide.Play, if (isPlaying) "Pause" else "Play") }
            IconButton(onClick = onRestore) { Icon(Lucide.Eye, "Restore studio controls") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudioFocusControlsPreview() {
    StudioFocusControls(false, {}, {})
}
