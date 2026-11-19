package com.donnclab.kinetiframe.presentation.studio.dock

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
import androidx.compose.ui.res.stringResource
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
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.toOffset
import com.donnclab.kinetiframe.presentation.common.toPoint2
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import kotlin.math.roundToInt

@Composable
fun SelectionDock(modifier: Modifier = Modifier, state: StudioState, onIntent: (StudioIntent) -> Unit) {
    var optionsVisible by remember { mutableStateOf(false) }
    FloatingDock(
        modifier = modifier,
        initialOffset = state.canvas.selectionDockOffset.toOffset(),
        onOffsetChange = { onIntent(StudioIntent.Selection.SetDockPosition(it.toPoint2())) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.spaceXs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            IconButton(onClick = { onIntent(StudioIntent.Selection.SetActive(false)) }) {
                Icon(Lucide.Check, stringResource(R.string.selection_apply), modifier = Modifier.size(Dimens.iconM))
            }
            Box {
                IconButton(onClick = { optionsVisible = true }) {
                    Icon(
                        Lucide.Ellipsis,
                        stringResource(R.string.selection_actions),
                        modifier = Modifier.size(Dimens.iconM),
                    )
                }
                SelectionActionMenu(optionsVisible, { optionsVisible = false }, state, onIntent)
            }
            IconButton(onClick = { onIntent(StudioIntent.Selection.Cancel) }) {
                Icon(Lucide.X, stringResource(R.string.action_cancel), modifier = Modifier.size(Dimens.iconM))
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
