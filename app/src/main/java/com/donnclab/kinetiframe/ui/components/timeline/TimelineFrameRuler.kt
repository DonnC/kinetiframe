package com.donnclab.kinetiframe.ui.components.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Compose only nearby labels, even when a recording spans thousands of frames. */
@Composable
internal fun TimelineFrameRuler(frames: Int, offsetFrames: Float, visibleFrames: Float, onSeekFrame: (Int) -> Unit) {
    val first = offsetFrames.toInt().coerceAtLeast(0)
    val last = (first + visibleFrames.toInt() + 2).coerceAtMost(frames - 1)
    Box(Modifier.fillMaxWidth().height(24.dp)) {
        for (frame in first..last) {
            Text(
                "${frame + 1}",
                modifier = Modifier.offset(
                    x = FRAME_CELL_WIDTH * (frame - offsetFrames),
                ).width(FRAME_CELL_WIDTH).clickable {
                    onSeekFrame(frame)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
