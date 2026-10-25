package com.donnclab.kinetiframe.presentation.actors.rigging

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Shared compact action with an accessible name and discoverable long-press tooltip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorToolButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(
            checked = selected,
            onCheckedChange = { onClick() },
            enabled = enabled,
            modifier = Modifier.size(40.dp),
        ) { Icon(icon, label, modifier = Modifier.size(18.dp)) }
    }
}

@Preview
@Composable
private fun ActorToolButtonPreview() {
    ActorToolButton(Icons.Default.TouchApp, "Fit joint") {}
}
