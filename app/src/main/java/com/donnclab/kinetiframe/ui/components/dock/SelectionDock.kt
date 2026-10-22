package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import kotlin.math.roundToInt

@Composable
fun SelectionDock(modifier: Modifier = Modifier, state: StudioState, onIntent: (StudioIntent) -> Unit) {
    var optionsVisible by remember { mutableStateOf(false) }
    FloatingDock(
        modifier = modifier,
        initialOffset = state.canvas.selectionDockOffset,
        onOffsetChange = { onIntent(StudioIntent.Selection.SetDockPosition(it)) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onIntent(StudioIntent.Selection.SetActive(false)) }) {
                Icon(Lucide.Check, "Apply selection", modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { optionsVisible = true }) {
                    Icon(Lucide.Ellipsis, "Selection actions", modifier = Modifier.size(20.dp))
                }
                SelectionActionMenu(optionsVisible, { optionsVisible = false }, state, onIntent)
            }
            IconButton(onClick = { onIntent(StudioIntent.Selection.Cancel) }) {
                Icon(Lucide.X, "Cancel", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun SelectionDockPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        SelectionDock(state = StudioState(), onIntent = {})
    }
}
