// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState

/**
 * One tool's settings. The phone dock shows them in a popup beside the rail; the tablet workspace
 * shows them in its tool panel, always open.
 */
@Composable
fun ToolOptionsContent(state: StudioState, tool: DrawingTool, onIntent: (StudioIntent) -> Unit, onDismiss: () -> Unit) {
    when (tool) {
        DrawingTool.BRUSH, DrawingTool.COLOR_FILL -> BrushOptionsContent(state, tool, onIntent)
        DrawingTool.ERASER -> EraserOptionsContent(state, onIntent, onDismiss)
        DrawingTool.LASSO -> SelectionOptionsContent(state, onIntent, onDismiss)
        DrawingTool.TEXT -> TypographyOptionsPanel(state, onIntent, onDismiss = onDismiss)
        DrawingTool.SMUDGE, DrawingTool.BLUR -> SmudgeBlurOptionsContent(state, tool, onIntent)
        else -> Box(Modifier.padding(Dimens.spaceL)) { Text(stringResource(R.string.tool_options)) }
    }
}
