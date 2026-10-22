package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.ui.CanvasState
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.components.StandardSlider

@Composable
fun GridOptionsContent(state: StudioState, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    val gridThemeColor = MaterialTheme.colorScheme.tertiary
    Column(Modifier.padding(16.dp).fillMaxWidth()) {
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
                    "Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EditorTokens.dark,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onIntent(StudioIntent.View.ResetGridSettings) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset grid",
                        modifier = Modifier.size(16.dp),
                        tint = EditorTokens.muted,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = state.canvas.gridEnabled,
                    onCheckedChange = { onIntent(StudioIntent.View.SetGridEnabled(it)) },
                    modifier = Modifier.scale(0.72f),
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EditorTokens.accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = EditorTokens.inactive.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Visual Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F8F8)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (state.canvas.gridEnabled) {
                    val gridColor = gridThemeColor.copy(alpha = state.canvas.gridOpacity.coerceIn(0.1f, 1f))
                    // Preview spacing is scaled down significantly to show the pattern
                    val previewScale = 0.15f
                    val vSpacing = (state.canvas.gridSpacingVertical * previewScale).coerceAtLeast(6f)
                    val hSpacing = (state.canvas.gridSpacingHorizontal * previewScale).coerceAtLeast(6f)

                    for (x in 0..(size.width / vSpacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x * vSpacing, 0f),
                            end = Offset(x * vSpacing, size.height),
                            strokeWidth = 1f,
                        )
                    }
                    for (y in 0..(size.height / hSpacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y * hSpacing),
                            end = Offset(size.width, y * hSpacing),
                            strokeWidth = 1f,
                        )
                    }
                } else {
                    drawRect(Color.Gray.copy(alpha = 0.05f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        GridAdjusterRow(
            label = "Opacity",
            value = state.canvas.gridOpacity,
            valueDisplay = "${(state.canvas.gridOpacity * 100).toInt()}%",
            range = 0.05f..1f,
            onValueChange = { onIntent(StudioIntent.View.SetGridOpacity(it)) },
        )

        Spacer(Modifier.height(20.dp))

        GridAdjusterRow(
            label = "Vertical Spacing",
            value = state.canvas.gridSpacingVertical,
            valueDisplay = "${state.canvas.gridSpacingVertical.toInt()}px",
            range = 10f..250f,
            onValueChange = { onIntent(StudioIntent.View.SetGridSpacingVertical(it)) },
        )

        Spacer(Modifier.height(20.dp))

        GridAdjusterRow(
            label = "Horizontal Spacing",
            value = state.canvas.gridSpacingHorizontal,
            valueDisplay = "${state.canvas.gridSpacingHorizontal.toInt()}px",
            range = 10f..250f,
            onValueChange = { onIntent(StudioIntent.View.SetGridSpacingHorizontal(it)) },
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GridAdjusterRow(
    label: String,
    value: Float,
    valueDisplay: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    StandardSlider(
        label = label,
        value = value,
        onValueChange = onValueChange,
        range = range,
        valueDisplay = { valueDisplay },
    )
}

@Preview(showBackground = true, widthDp = 280)
@Composable
private fun GridOptionsPreview() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
    ) {
        GridOptionsContent(
            state = StudioState(
                canvas = CanvasState(
                    gridEnabled = true,
                    gridOpacity = 0.4f,
                    gridSpacingVertical = 120f,
                    gridSpacingHorizontal = 120f,
                ),
            ),
            onIntent = {},
            onDismiss = {},
        )
    }
}
