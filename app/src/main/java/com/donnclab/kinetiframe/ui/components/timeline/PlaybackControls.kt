package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.ui.StudioIntent

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onIntent: (StudioIntent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                IconButton(onClick = {
                    onIntent(StudioIntent.Playback.PreviousFrame)
                }, modifier = Modifier.size(40.dp, 48.dp)) {
                    Icon(Lucide.SkipBack, "Prev", tint = EditorTokens.dark, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(
                onClick = {
                    if (isPlaying) onIntent(StudioIntent.Playback.Pause) else onIntent(StudioIntent.Playback.Play)
                },
                modifier = Modifier.size(40.dp, 48.dp),
            ) {
                Icon(
                    if (isPlaying) Lucide.Pause else Lucide.Play,
                    if (isPlaying) "Pause" else "Play",
                    tint = EditorTokens.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (!compact) {
                IconButton(onClick = {
                    onIntent(StudioIntent.Playback.NextFrame)
                }, modifier = Modifier.size(40.dp, 48.dp)) {
                    Icon(Lucide.SkipForward, "Next", tint = EditorTokens.dark, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaybackControlsPreview() {
    PlaybackControls(false, {})
}
