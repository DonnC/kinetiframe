// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Lasso
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Type
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.studio.color.QuickSwatches
import com.donnclab.kinetiframe.presentation.studio.dock.ColorPicker
import com.donnclab.kinetiframe.presentation.studio.dock.ToolOptionsContent
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ToolConfig
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape

/** A tool in the workspace panel: what it is called, how it looks, and how it is chosen. */
private data class WorkspaceTool(
    val icon: ImageVector,
    val label: Int,
    val selected: (StudioState) -> Boolean,
    val choose: (StudioState) -> StudioIntent,
)

private val tools = listOf(
    WorkspaceTool(Lucide.Pen, R.string.tool_pen, {
        it.tool.activeTool == DrawingTool.BRUSH &&
            it.tool.constructionShape == null
    }) {
        StudioIntent.Draw.SelectTool(DrawingTool.BRUSH)
    },
    WorkspaceTool(Lucide.Shapes, R.string.workspace_tool_shapes, { it.tool.constructionShape != null }) {
        StudioIntent.Draw.SelectConstructionShape(it.tool.constructionShape ?: ConstructionShape.LINE)
    },
    WorkspaceTool(Lucide.Eraser, R.string.tool_eraser, { it.tool.activeTool == DrawingTool.ERASER }) {
        StudioIntent.Draw.SelectTool(DrawingTool.ERASER)
    },
    WorkspaceTool(Lucide.Lasso, R.string.tool_lasso, { it.tool.activeTool == DrawingTool.LASSO }) {
        StudioIntent.Draw.SelectTool(DrawingTool.LASSO)
    },
    WorkspaceTool(Lucide.PaintBucket, R.string.tool_fill, { it.tool.activeTool == DrawingTool.COLOR_FILL }) {
        StudioIntent.Draw.SelectTool(DrawingTool.COLOR_FILL)
    },
    WorkspaceTool(Lucide.Type, R.string.tool_text, { it.tool.activeTool == DrawingTool.TEXT }) {
        StudioIntent.Draw.SelectTool(DrawingTool.TEXT)
    },
)

private const val TOOLS_PER_ROW = 3

/**
 * The tablet workspace's left panel: the tools as labelled tiles, the current colour with quick
 * swatches, and the chosen tool's settings, always open. On a phone the same settings live in a
 * narrow popup beside a floating rail; a tablet has room to keep them in view while drawing.
 */
@Composable
internal fun WorkspaceToolPanel(state: StudioState, onIntent: (StudioIntent) -> Unit, modifier: Modifier = Modifier) {
    WorkspacePanelSurface(modifier.width(EditorTokens.workspaceToolPanelWidth)) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(Dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            PanelHeading(stringResource(R.string.workspace_tools))
            tools.chunked(TOOLS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                    row.forEach { tool ->
                        ToolTile(tool.icon, stringResource(tool.label), tool.selected(state), Modifier.weight(1f)) {
                            onIntent(tool.choose(state))
                        }
                    }
                }
            }
            HorizontalDivider()
            PanelHeading(stringResource(R.string.workspace_colour))
            val colour = Color((state.tool.configs[DrawingTool.BRUSH] ?: ToolConfig()).color)
            ColorPicker(colour) { onIntent(StudioIntent.Draw.SetBrushColor(it.toArgb())) }
            QuickSwatches(colour) { onIntent(StudioIntent.Draw.SetBrushColor(it.toArgb())) }
            HorizontalDivider()
            ToolOptionsContent(state, state.tool.activeTool, onIntent, onDismiss = {})
        }
    }
}

@Composable
private fun ToolTile(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusM),
        color = if (selected) EditorTokens.accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(Dimens.borderThin, if (selected) EditorTokens.accent else Color.Transparent),
    ) {
        Column(
            Modifier.padding(vertical = Dimens.spaceS),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            Icon(
                icon,
                null,
                Modifier.size(Dimens.iconM),
                tint = if (selected) EditorTokens.accent else EditorTokens.dark,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
internal fun PanelHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = EditorTokens.dark)
}

/** The rounded, raised card each workspace panel sits on. */
@Composable
internal fun WorkspacePanelSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight().padding(Dimens.spaceS),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusM),
        tonalElevation = Dimens.elevationRaised,
        border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.2f)),
    ) { content() }
}
