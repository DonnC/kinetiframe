package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SquarePlus
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent

@Composable
fun TimelineActionRow(
    layerCount: Int,
    toolbarVisible: Boolean,
    onIntent: (StudioIntent) -> Unit,
    onToolbarToggle: () -> Unit,
    onLayerPanelToggle: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        var open by remember { mutableStateOf(false) }
        Box(modifier) {
            IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.Ellipsis, "Frame and canvas actions")
            }
            DropdownMenu(open, { open = false }) {
                TimelineActionRow(
                    layerCount,
                    toolbarVisible,
                    {
                        open = false
                        onIntent(it)
                    },
                    {
                        open = false
                        onToolbarToggle()
                    },
                    {
                        open = false
                        onLayerPanelToggle()
                    },
                )
            }
        }
        return
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Add Frame
        IconButton(
            onClick = { onIntent(StudioIntent.Frame.AddFrame) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Lucide.SquarePlus,
                contentDescription = "Add Frame",
                tint = EditorTokens.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        // Visibility Toggle
        IconButton(
            onClick = onToolbarToggle,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                if (toolbarVisible) Lucide.Eye else Lucide.EyeOff,
                contentDescription = "Focus canvas",
                tint = if (toolbarVisible) EditorTokens.accent else EditorTokens.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        // Layer Panel Toggle
        IconButton(
            onClick = onLayerPanelToggle,
            modifier = Modifier.size(48.dp),
        ) {
            Box {
                Icon(
                    Lucide.Layers,
                    contentDescription = "Layers",
                    tint = EditorTokens.accent,
                    modifier = Modifier.size(18.dp),
                )
                if (layerCount > 0) {
                    Surface(
                        color = EditorTokens.accent,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(14.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$layerCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineActionRowPreview() {
    TimelineActionRow(2, true, {}, {}, {})
}
