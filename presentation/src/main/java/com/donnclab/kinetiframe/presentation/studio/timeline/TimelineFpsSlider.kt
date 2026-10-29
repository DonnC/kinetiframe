package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TimelineFpsSlider(fps: Int, onIntent: (StudioIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp),
    ) {
        Text("FPS", color = EditorTokens.muted, style = MaterialTheme.typography.labelSmall)
        Slider(
            value = fps.toFloat(),
            onValueChange = { onIntent(StudioIntent.Playback.SetFps(it.toInt())) },
            valueRange = 1f..30f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            thumb = { Box(Modifier.size(12.dp).clip(CircleShape).background(EditorTokens.accent)) },
            track = { _ ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(EditorTokens.accent.copy(alpha = 0.2f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction = (fps - 1f) / 29f)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(EditorTokens.accent),
                    )
                }
            },
        )
        Text(
            "$fps",
            color = EditorTokens.dark,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(20.dp),
        )
    }
}
