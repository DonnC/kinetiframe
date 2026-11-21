package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R

/** "Raster Layers" group label above the actor's drawable layers in [PuppetInspector]. */
@Composable
fun RasterLayerGroupHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = Dimens.spaceL, top = Dimens.spaceXs, bottom = Dimens.spaceXs),
    ) {
        Icon(
            Lucide.FolderOpen,
            null,
            tint = ActorStudioTokens.onPanelMuted,
            modifier = Modifier.size(Dimens.iconS).padding(end = Dimens.spaceXs),
        )
        Text(
            stringResource(R.string.raster_layers_title),
            style = MaterialTheme.typography.bodySmall,
            color = ActorStudioTokens.onPanelMuted,
        )
    }
}

/** One selectable, visibility-toggleable raster layer row in [PuppetInspector]'s layer list. */
@Composable
fun RasterLayerRow(
    name: String,
    isActive: Boolean,
    isVisible: Boolean,
    onClick: () -> Unit,
    onVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ActorStudioTokens.rasterIndent, top = Dimens.spaceXxs, bottom = Dimens.spaceXxs)
            .clickable(onClick = onClick)
            .background(
                if (isActive) ActorStudioTokens.panelSurfaceVariant else Color.Transparent,
                RoundedCornerShape(Dimens.radiusXxs),
            )
            .padding(horizontal = Dimens.spaceXsPlus, vertical = Dimens.spaceXs),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = if (isActive) ActorStudioTokens.onPanel else ActorStudioTokens.onPanelMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            if (isVisible) Lucide.Eye else Lucide.EyeOff,
            contentDescription = stringResource(
                if (isVisible) R.string.raster_layer_hide else R.string.raster_layer_show,
                name,
            ),
            tint = if (isVisible) ActorStudioTokens.onPanelMuted else ActorStudioTokens.accent,
            modifier = Modifier.size(ActorStudioTokens.treeSmallIcon).clickable(onClick = onVisibilityClick),
        )
    }
}
