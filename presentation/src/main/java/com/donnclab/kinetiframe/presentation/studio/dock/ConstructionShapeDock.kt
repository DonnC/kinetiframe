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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.toOffset
import com.donnclab.kinetiframe.presentation.common.toPoint2
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/** Same confirmation affordance as Fill: preview first, then explicit commit or cancel. */
@Composable
fun ConstructionShapeDock(state: StudioState, onIntent: (StudioIntent) -> Unit) {
    FloatingDock(
        initialOffset = state.constructionShape.dockOffset.toOffset(),
        onOffsetChange = { onIntent(StudioIntent.Construction.SetDockPosition(it.toPoint2())) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.spaceXs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            IconButton(onClick = { onIntent(StudioIntent.Construction.Confirm) }) {
                Icon(
                    Lucide.Check,
                    stringResource(R.string.shape_confirm),
                    modifier = Modifier.size(EditorTokens.confirmIcon),
                    tint = EditorTokens.accent,
                )
            }
            IconButton(onClick = { onIntent(StudioIntent.Construction.Cancel) }) {
                Icon(Lucide.X, stringResource(R.string.shape_cancel), modifier = Modifier.size(Dimens.iconM))
            }
        }
    }
}
