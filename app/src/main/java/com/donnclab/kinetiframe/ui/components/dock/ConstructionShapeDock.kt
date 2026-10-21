package com.donnclab.kinetiframe.ui.components.dock

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
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState

/** Same confirmation affordance as Fill: preview first, then explicit commit or cancel. */
@Composable
fun ConstructionShapeDock(state: TimelineState, onIntent: (TimelineIntent) -> Unit) {
    FloatingDock(
        initialOffset = state.constructionShape.dockOffset,
        onOffsetChange = { onIntent(TimelineIntent.Construction.SetDockPosition(it)) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { onIntent(TimelineIntent.Construction.Confirm) }) {
                Icon(Lucide.Check, "Confirm shape", modifier = Modifier.size(22.dp), tint = EditorTokens.accent)
            }
            IconButton(onClick = { onIntent(TimelineIntent.Construction.Cancel) }) {
                Icon(Lucide.X, "Cancel shape", modifier = Modifier.size(20.dp))
            }
        }
    }
}
