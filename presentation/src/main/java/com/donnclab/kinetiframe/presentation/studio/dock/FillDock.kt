package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

@Composable
fun FillDock(modifier: Modifier = Modifier, state: StudioState, onIntent: (StudioIntent) -> Unit) {
    FloatingDock(
        modifier = modifier,
        initialOffset = state.fill.dockOffset,
        onOffsetChange = { onIntent(StudioIntent.Fill.SetDockPosition(it)) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onIntent(StudioIntent.Fill.Confirm) }) {
                Icon(Lucide.Check, "Confirm Fill", modifier = Modifier.size(22.dp), tint = EditorTokens.accent)
            }
            IconButton(onClick = { onIntent(StudioIntent.Fill.Cancel) }) {
                Icon(Lucide.X, "Cancel", modifier = Modifier.size(20.dp))
            }
        }
    }
}
