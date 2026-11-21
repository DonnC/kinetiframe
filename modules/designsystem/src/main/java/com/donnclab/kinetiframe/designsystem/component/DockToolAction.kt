package com.donnclab.kinetiframe.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

data class DockToolAction(val icon: ImageVector, val label: String, val onSelect: () -> Unit)
