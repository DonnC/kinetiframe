package com.donnclab.kinetiframe.ui.actors.sketch

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PencilRuler
import com.composables.icons.lucide.Text

@Composable
fun ActorDrawingModeButton(
    hasDrawing: Boolean,
    drawingEnabled: Boolean,
    onSketch: () -> Unit,
    onMode: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) { Icon(Lucide.PencilRuler, "Actor drawing workflow") }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = {
                Text(if (hasDrawing) "Edit continuous drawing" else "Draw & Animate")
            }, onClick = {
                expanded =
                    false
                onSketch()
            })
            if (hasDrawing) {
                DropdownMenuItem(text = {
                    Text(if (drawingEnabled) "Use cutout artwork" else "Use continuous drawing")
                }, onClick = {
                    expanded =
                        false
                    onMode(!drawingEnabled)
                })
            }
        }
    }
}

@Preview
@Composable
private fun DrawingModePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme { ActorDrawingModeButton(true, true, {}, {}) }
}
