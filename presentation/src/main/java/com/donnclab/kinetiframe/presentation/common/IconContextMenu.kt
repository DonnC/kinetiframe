package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Whether context menus show a label under each icon. The app provides the user's setting. */
val LocalShowMenuLabels = staticCompositionLocalOf { true }

data class ContextIconAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val selected: Boolean? = null,
)

/** Small action palette. Words live in tooltips and accessibility descriptions, not giant menu rows. */
@Composable
fun IconContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<ContextIconAction>,
    additionalContent: (@Composable () -> Unit)? = null,
) {
    val showLabels = LocalShowMenuLabels.current
    DropdownMenu(expanded, onDismissRequest = onDismiss) {
        actions.chunked(4).forEach { row -> Row { row.forEach { ContextMenuActionTile(it, showLabels) } } }
        additionalContent?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextIconButton(action: ContextIconAction, modifier: Modifier = Modifier) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(action.label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = action.onClick,
            enabled = action.enabled,
            modifier = modifier.semantics {
                action.selected?.let {
                    stateDescription = if (it) "On" else "Off"
                    toggleableState = if (it) ToggleableState.On else ToggleableState.Off
                    role = Role.Switch
                }
            },
        ) {
            Icon(
                action.icon,
                action.label,
                tint = when {
                    !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    action.selected == true -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconMenuPreview() {
    KinetiFrameTheme { ContextIconButton(ContextIconAction(Icons.Filled.ContentCopy, "Copy", {})) }
}
