package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Scissors
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.SelectionInteractionMode
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

@Composable
fun SelectionActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
) {
    DropdownMenu(expanded, onDismissRequest = onDismiss) {
        Row {
            SelectionAction(Lucide.Move, "Move selection", {
                onDismiss()
                onIntent(StudioIntent.Selection.SetInteractionMode(SelectionInteractionMode.MOVE))
            }, state.selection.interactionMode == SelectionInteractionMode.MOVE, toggle = true)
            SelectionAction(Icons.Filled.Transform, "Transform selection", {
                onDismiss()
                onIntent(StudioIntent.Selection.SetInteractionMode(SelectionInteractionMode.TRANSFORM))
            }, state.selection.interactionMode == SelectionInteractionMode.TRANSFORM, toggle = true)
        }
        Row {
            SelectionAction(Lucide.Copy, "Copy", {
                onDismiss()
                onIntent(StudioIntent.Selection.Copy)
            })
            SelectionAction(Lucide.Scissors, "Cut", {
                onDismiss()
                onIntent(StudioIntent.Selection.Cut)
            })
            SelectionAction(Lucide.Trash2, "Delete", {
                onDismiss()
                onIntent(StudioIntent.Selection.Delete)
            })
        }
        Row {
            SelectionAction(Icons.Filled.AspectRatio, "Lock proportions", {
                onIntent(StudioIntent.Selection.SetAspectLock(!state.selection.lockAspectRatio))
            }, state.selection.lockAspectRatio, toggle = true)
            SelectionAction(Icons.Filled.Transform, "Skew side handles", {
                onIntent(StudioIntent.Selection.SetSkewMode(!state.selection.skewMode))
            }, state.selection.skewMode, toggle = true)
            SelectionAction(Lucide.RotateCcw, "Reset transform", {
                onDismiss()
                onIntent(StudioIntent.Selection.ResetTransform)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    toggle: Boolean = false,
) {
    com.donnclab.kinetiframe.presentation.common.ContextIconButton(
        com.donnclab.kinetiframe.presentation.common.ContextIconAction(
            icon,
            label,
            onClick,
            selected = if (toggle) selected else null,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun SelectionActionsPreview() {
    KinetiFrameTheme { SelectionActionMenu(true, {}, StudioState(), {}) }
}
