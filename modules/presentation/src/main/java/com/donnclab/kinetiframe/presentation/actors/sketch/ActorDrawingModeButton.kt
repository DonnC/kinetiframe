package com.donnclab.kinetiframe.presentation.actors.sketch

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PencilRuler
import com.composables.icons.lucide.Text
import com.donnclab.kinetiframe.presentation.R

@Composable
fun ActorDrawingModeButton(
    hasDrawing: Boolean,
    drawingEnabled: Boolean,
    onSketch: () -> Unit,
    onMode: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) { Icon(Lucide.PencilRuler, stringResource(R.string.sketch_workflow)) }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = {
                Text(stringResource(if (hasDrawing) R.string.sketch_edit_drawing else R.string.sketch_draw_and_animate))
            }, onClick = {
                expanded =
                    false
                onSketch()
            })
            if (hasDrawing) {
                DropdownMenuItem(text = {
                    Text(
                        stringResource(
                            if (drawingEnabled) R.string.sketch_use_cutouts else R.string.sketch_use_drawing,
                        ),
                    )
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
