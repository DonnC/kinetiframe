package com.donnclab.kinetiframe.studio.ui.dock

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.donnclab.kinetiframe.designsystem.component.DockToolAction
import com.donnclab.kinetiframe.designsystem.component.DockToolButton

@Composable
fun ToolButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, onOpenMenu: () -> Unit) {
    DockToolButton(DockToolAction(icon, label, onClick), selected, onOpenMenu)
}
