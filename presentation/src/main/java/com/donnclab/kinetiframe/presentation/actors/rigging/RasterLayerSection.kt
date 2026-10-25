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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens

/** "Raster Layers" group label above the actor's drawable layers in [PuppetInspector]. */
@Composable
fun RasterLayerGroupHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Icon(
            Lucide.FolderOpen,
            null,
            tint = ActorStudioTokens.onPanelMuted,
            modifier = Modifier.size(16.dp).padding(end = 4.dp),
        )
        Text("Raster Layers", style = MaterialTheme.typography.bodySmall, color = ActorStudioTokens.onPanelMuted)
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
            .padding(start = 24.dp, top = 2.dp, bottom = 2.dp)
            .clickable(onClick = onClick)
            .background(
                if (isActive) ActorStudioTokens.panelSurfaceVariant else Color.Transparent,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
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
            contentDescription = if (isVisible) "Hide $name" else "Show $name",
            tint = if (isVisible) ActorStudioTokens.onPanelMuted else ActorStudioTokens.accent,
            modifier = Modifier.size(14.dp).clickable(onClick = onVisibilityClick),
        )
    }
}
