package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.preview.PreviewCatalog

/** One row in an Audio tab's list: a play button, the title, duration and a small kind tag. */
@Composable
fun AudioTrackRow(track: AudioTrackSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.radiusL)) {
        Row(
            Modifier.padding(Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            val playGradient = Brush.linearGradient(listOf(Color(0xFFFFD9CC), MaterialTheme.colorScheme.primary))
            Box(
                Modifier.size(Dimens.playButtonSize).clip(CircleShape).background(playGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AudioFile, contentDescription = "Audio details and preview", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                    Text(
                        track.durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AudioTag(track.tagLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioTrackRowPreview() {
    KinetiFrameTheme { AudioTrackRow(PreviewCatalog.libraryAudio().first(), onClick = {}) }
}
