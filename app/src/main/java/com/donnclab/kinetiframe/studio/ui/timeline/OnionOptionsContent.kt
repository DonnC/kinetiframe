package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.common.StandardSlider
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ui.dock.ColorPicker

@Composable
fun OnionOptionsContent(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    val accentColor = EditorTokens.accent
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        // Master Switch at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = EditorTokens.dark,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Onion Skin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EditorTokens.dark,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onIntent(StudioIntent.OnionSkin.Reset) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset onion skin",
                        modifier = Modifier.size(16.dp),
                        tint = accentColor,
                    )
                }
                Switch(
                    checked = state.onionSkin.enabled,
                    onCheckedChange = { onIntent(StudioIntent.OnionSkin.SetEnabled(it)) },
                    modifier = Modifier.scale(0.72f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = EditorTokens.inactive.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Visual Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F8F8)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                if (state.onionSkin.enabled) {
                    val previewWidth = size.width * 0.4f
                    val previewHeight = size.height * 0.6f
                    val centerY = size.height / 2f
                    val centerX = size.width / 2f

                    // Draw Before frames
                    for (i in 1..state.onionSkin.depthBefore) {
                        val offset = i * 20f
                        val alpha = (state.onionSkin.opacity / i).coerceIn(0.1f, 1f)
                        drawRect(
                            color = state.onionSkin.colorBefore.copy(alpha = alpha),
                            topLeft = Offset(centerX - previewWidth / 2f - offset, centerY - previewHeight / 2f),
                            size = Size(previewWidth, previewHeight),
                            style = Stroke(width = 2f),
                        )
                    }

                    // Draw After frames
                    for (i in 1..state.onionSkin.depthAfter) {
                        val offset = i * 20f
                        val alpha = (state.onionSkin.opacity / i).coerceIn(0.1f, 1f)
                        drawRect(
                            color = state.onionSkin.colorAfter.copy(alpha = alpha),
                            topLeft = Offset(centerX - previewWidth / 2f + offset, centerY - previewHeight / 2f),
                            size = Size(previewWidth, previewHeight),
                            style = Stroke(width = 2f),
                        )
                    }

                    // Current Frame
                    drawRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset(centerX - previewWidth / 2f, centerY - previewHeight / 2f),
                        size = Size(previewWidth, previewHeight),
                        style = Stroke(width = 3f),
                    )
                } else {
                    drawRect(Color.Gray.copy(alpha = 0.05f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OnionAdjusterRow(
            label = "Opacity",
            value = state.onionSkin.opacity,
            valueDisplay = "${(state.onionSkin.opacity * 100).toInt()}%",
            range = 0.05f..1f,
            onValueChange = { onIntent(StudioIntent.OnionSkin.SetOpacity(it)) },
        )

        Spacer(Modifier.height(12.dp))

        OnionAdjusterRow(
            label = "Frames Before",
            value = state.onionSkin.depthBefore.toFloat(),
            valueDisplay = "${state.onionSkin.depthBefore}",
            range = 0f..4f,
            steps = 3,
            onValueChange = { onIntent(StudioIntent.OnionSkin.SetFramesBefore(it.toInt())) },
        )

        Spacer(Modifier.height(12.dp))

        OnionAdjusterRow(
            label = "Frames After",
            value = state.onionSkin.depthAfter.toFloat(),
            valueDisplay = "${state.onionSkin.depthAfter}",
            range = 0f..4f,
            steps = 3,
            onValueChange = { onIntent(StudioIntent.OnionSkin.SetFramesAfter(it.toInt())) },
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Before",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorTokens.muted,
                    fontWeight = FontWeight.SemiBold,
                )
                ColorPicker(state.onionSkin.colorBefore) { onIntent(StudioIntent.OnionSkin.SetColorBefore(it)) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "After",
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorTokens.muted,
                    fontWeight = FontWeight.SemiBold,
                )
                ColorPicker(state.onionSkin.colorAfter) { onIntent(StudioIntent.OnionSkin.SetColorAfter(it)) }
            }
        }
    }
}

@Composable
private fun OnionAdjusterRow(
    label: String,
    value: Float,
    valueDisplay: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    StandardSlider(
        label = label,
        value = value,
        onValueChange = onValueChange,
        range = range,
        steps = steps,
        valueDisplay = { valueDisplay },
    )
}
