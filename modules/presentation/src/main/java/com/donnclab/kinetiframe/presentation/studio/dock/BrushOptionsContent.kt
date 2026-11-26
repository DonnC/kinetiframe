package com.donnclab.kinetiframe.presentation.studio.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.FlipHorizontal
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Pill
import com.composables.icons.lucide.Slash
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Triangle
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.core.document.BrushCatalog
import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.StandardSlider
import com.donnclab.kinetiframe.presentation.common.valueText
import com.donnclab.kinetiframe.studio.RulerMode
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.ToolConfig
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape

@Composable
fun BrushOptionsContent(state: StudioState, tool: DrawingTool, onIntent: (StudioIntent) -> Unit) {
    val config = if (state.tool.constructionShape !=
        null
    ) {
        state.tool.constructionConfig
    } else {
        state.tool.configs[tool] ?: ToolConfig()
    }
    var brushPickerExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(Dimens.spaceM).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val title = when (tool) {
                DrawingTool.BRUSH -> stringResource(R.string.tool_brush)
                DrawingTool.COLOR_FILL -> stringResource(R.string.tool_fill)
                else -> stringResource(R.string.tool_generic)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            ColorPicker(Color(config.color)) { onIntent(StudioIntent.Draw.SetBrushColor(it.toArgb())) }
        }

        if (tool == DrawingTool.BRUSH) {
            Spacer(modifier = Modifier.height(EditorTokens.previewGap))
            BrushPickerRow(
                brush = config.brushId,
                expanded = brushPickerExpanded,
                onClick = { brushPickerExpanded = true },
                onSelect = {
                    onIntent(StudioIntent.Draw.SelectBrush(it))
                    brushPickerExpanded = false
                },
                onDismiss = { brushPickerExpanded = false },
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceM))

        StandardSlider(
            label = stringResource(if (tool == DrawingTool.COLOR_FILL) R.string.tool_tolerance else R.string.tool_size),
            value = config.size,
            onValueChange = {
                onIntent(StudioIntent.Draw.SetBrushSize(it))
            },
            range = 1f..100f,
        )

        Spacer(modifier = Modifier.height(Dimens.spaceM))

        StandardSlider(
            label = stringResource(R.string.tool_opacity),
            value = config.opacity,
            onValueChange = {
                onIntent(StudioIntent.Draw.SetBrushOpacity(it))
            },
            range = 0.01f..1f,
            valueDisplay = valueText(R.string.value_percent) { (it * 100).toInt() },
        )

        if (tool != DrawingTool.COLOR_FILL && state.tool.constructionShape == null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceM))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolIconButton(
                    Lucide.Minus,
                    stringResource(R.string.ruler_line),
                    state.canvas.rulerMode == RulerMode.LINE,
                ) {
                    onIntent(StudioIntent.View.SetRulerMode(RulerMode.LINE))
                }
                ToolIconButton(
                    Lucide.Circle,
                    stringResource(R.string.ruler_circle),
                    state.canvas.rulerMode == RulerMode.CIRCLE,
                ) {
                    onIntent(StudioIntent.View.SetRulerMode(RulerMode.CIRCLE))
                }
                ToolIconButton(
                    Lucide.Square,
                    stringResource(R.string.ruler_box),
                    state.canvas.rulerMode == RulerMode.BOX,
                ) {
                    onIntent(StudioIntent.View.SetRulerMode(RulerMode.BOX))
                }
                ToolIconButton(
                    Lucide.FlipHorizontal,
                    stringResource(R.string.ruler_mirror),
                    state.canvas.rulerMode == RulerMode.MIRROR,
                ) {
                    onIntent(StudioIntent.View.SetRulerMode(RulerMode.MIRROR))
                }
                ToolIconButton(Lucide.X, stringResource(R.string.ruler_off), state.canvas.rulerMode == RulerMode.NONE) {
                    onIntent(StudioIntent.View.SetRulerMode(RulerMode.NONE))
                }
            }
        } else if (state.tool.constructionShape != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spaceM))
            // Wraps: seven shapes don't fit the panel's width in one row.
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                ToolIconButton(
                    Lucide.Slash,
                    stringResource(R.string.shape_line),
                    state.tool.constructionShape == ConstructionShape.LINE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.LINE))
                }
                ToolIconButton(
                    Lucide.Square,
                    stringResource(R.string.shape_rectangle),
                    state.tool.constructionShape == ConstructionShape.RECTANGLE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.RECTANGLE))
                }
                ToolIconButton(
                    Lucide.Circle,
                    stringResource(R.string.shape_ellipse),
                    state.tool.constructionShape == ConstructionShape.ELLIPSE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.ELLIPSE))
                }
                ToolIconButton(
                    Lucide.Pill,
                    stringResource(R.string.shape_capsule),
                    state.tool.constructionShape == ConstructionShape.CAPSULE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.CAPSULE))
                }
                ToolIconButton(
                    Lucide.Triangle,
                    stringResource(R.string.shape_triangle),
                    state.tool.constructionShape == ConstructionShape.TRIANGLE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.TRIANGLE))
                }
                ToolIconButton(
                    Lucide.CirclePlus,
                    stringResource(R.string.shape_head_guide),
                    state.tool.constructionShape == ConstructionShape.HEAD_GUIDE,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.HEAD_GUIDE))
                }
                ToolIconButton(
                    Lucide.Spline,
                    stringResource(R.string.shape_three_point_arc),
                    state.tool.constructionShape == ConstructionShape.THREE_POINT_ARC,
                ) {
                    onIntent(StudioIntent.Draw.SelectConstructionShape(ConstructionShape.THREE_POINT_ARC))
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(EditorTokens.control)) {
        Icon(
            icon,
            description,
            modifier = Modifier.size(EditorTokens.icon),
            tint = if (selected) EditorTokens.accent else EditorTokens.dark,
        )
    }
}

@Composable
private fun BrushPickerRow(
    brush: BrushId,
    expanded: Boolean,
    onClick: () -> Unit,
    onSelect: (BrushId) -> Unit,
    onDismiss: () -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.brush_type),
            style = MaterialTheme.typography.labelMedium,
            color = EditorTokens.dark,
        )
        Spacer(Modifier.height(EditorTokens.labelGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(EditorTokens.previewCorner))
                .background(EditorTokens.brushPreviewWell)
                .clickable(onClick = onClick)
                .padding(Dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(brushPreviewResource(brush)),
                contentDescription = stringResource(R.string.brush_named, brush.displayName),
                modifier = Modifier.size(EditorTokens.brushPreview),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.size(EditorTokens.previewGap))
            Text(
                brush.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = EditorTokens.brushPreviewText,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            BrushCatalog.drawingBrushes.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(brushPreviewResource(option)),
                                contentDescription = null,
                                modifier = Modifier.size(EditorTokens.brushMenuPreview),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.size(Dimens.spaceS))
                            Text(option.displayName)
                        }
                    },
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

private fun brushPreviewResource(brush: BrushId): Int = when (brush) {
    BrushId.PEN -> R.drawable.brush_pen
    BrushId.PENCIL -> R.drawable.brush_pencil
    BrushId.PAINT -> R.drawable.brush_paint
    BrushId.AIRBRUSH -> R.drawable.brush_airbrush
    BrushId.HIGHLIGHTER -> R.drawable.brush_highlighter
    BrushId.CRAYON -> R.drawable.brush_crayon
    BrushId.INK -> R.drawable.brush_ink
    BrushId.HAIR -> R.drawable.brush_hair
    BrushId.FUR -> R.drawable.brush_fur
    BrushId.SKETCH -> R.drawable.brush_sketch
    BrushId.CALLIGRAPHY -> R.drawable.brush_calligraphy
    BrushId.SPRAY -> R.drawable.brush_spray
    BrushId.STIPPLE -> R.drawable.brush_stipple
    BrushId.ROUGH_PEN -> R.drawable.brush_rough_pen
    BrushId.GRAIN_PEN -> R.drawable.brush_grain_pen
    BrushId.DASH -> R.drawable.brush_dash
    BrushId.GEAR -> R.drawable.brush_gear
    BrushId.SAW -> R.drawable.brush_saw
}

@Preview(showBackground = true)
@Composable
private fun BrushOptionsContentPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        BrushOptionsContent(StudioState(), DrawingTool.BRUSH, {})
    }
}
