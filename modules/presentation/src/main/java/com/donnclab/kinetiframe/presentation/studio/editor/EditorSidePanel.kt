// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens

/**
 * A panel docked beside the canvas on wider windows, such as the layers list. On a phone the same
 * content opens as a popup over the canvas; on a tablet there is room for it next to the drawing,
 * so it never covers the work.
 */
@Composable
internal fun EditorSidePanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight().width(EditorTokens.sidePanelWidth).padding(Dimens.spaceS),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusM),
        tonalElevation = Dimens.elevationRaised,
        border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.2f)),
    ) { content() }
}
