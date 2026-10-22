package com.donnclab.kinetiframe.studio.ui.timeline.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LockOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * The mute/lock gutter for one audio lane, sitting to the left of its clips.
 *
 * Both controls are lane-wide on purpose. Muting is a mixing decision about a whole voice ("drop
 * the ambience while I check the dialogue"), so it must not require touching -- and later
 * un-touching -- every clip on the lane. Locking freezes a lane whose timing is settled so work on
 * a neighbouring lane cannot nudge it; it deliberately does not silence, because a locked lane is
 * one you have finished moving, not one you have stopped listening to.
 *
 * Muted state is carried by the glyph itself rather than by tint alone: a merely dimmed icon is
 * indistinguishable from a disabled control, and this is the one place an animator needs to tell
 * "silent" from "unavailable" at a glance.
 */
@Composable
fun AudioLaneHeader(
    isMuted: Boolean,
    isLocked: Boolean,
    onToggleMute: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onToggleMute, modifier = Modifier.size(CONTROL_SIZE.dp)) {
            Icon(
                imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                contentDescription = if (isMuted) FeatureStrings.Audio.UNMUTE_LANE else FeatureStrings.Audio.MUTE_LANE,
                tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(GLYPH_SIZE.dp),
            )
        }
        IconButton(onClick = onToggleLock, modifier = Modifier.size(CONTROL_SIZE.dp)) {
            Icon(
                imageVector = if (isLocked) Lucide.Lock else Lucide.LockOpen,
                contentDescription = if (isLocked) FeatureStrings.Audio.UNLOCK_LANE else FeatureStrings.Audio.LOCK_LANE,
                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(GLYPH_SIZE.dp),
            )
        }
    }
}

private const val CONTROL_SIZE = 36
private const val GLYPH_SIZE = 15

@Preview(name = "Lane header -- light", showBackground = true)
@Preview(
    name = "Lane header -- dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AudioLaneHeaderPreview() {
    KinetiFrameTheme {
        AudioLaneHeader(isMuted = true, isLocked = false, onToggleMute = {}, onToggleLock = {})
    }
}
