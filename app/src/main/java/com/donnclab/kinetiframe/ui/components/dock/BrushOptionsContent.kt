package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.FlipHorizontal
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Pill
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Triangle
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.engine.drawing.BrushCatalog
import com.donnclab.kinetiframe.engine.drawing.BrushId
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.engine.shape.ConstructionShape
import com.donnclab.kinetiframe.ui.RulerMode
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.ToolConfig
import com.donnclab.kinetiframe.ui.components.StandardSlider

@Composable
fun BrushOptionsContent(state: TimelineState, tool: DrawingTool, onIntent: (TimelineIntent) -> Unit) {
    val config = if (state.tool.constructionShape !=
        null
    ) {
        state.tool.constructionConfig
    } else {
        state.tool.configs[tool] ?: ToolConfig()
    }
    var brushPickerExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val title = when (tool) {
                DrawingTool.BRUSH -> "Brush"
                DrawingTool.COLOR_FILL -> "Fill"
                else -> "Tool"
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            ColorPicker(config.color) { onIntent(TimelineIntent.Draw.SetBrushColor(it)) }
        }

        if (tool == DrawingTool.BRUSH) {
            Spacer(modifier = Modifier.height(10.dp))
            BrushPickerRow(
                brush = config.brushId,
                expanded = brushPickerExpanded,
                onClick = { brushPickerExpanded = true },
                onSelect = {
                    onIntent(TimelineIntent.Draw.SelectBrush(it))
                    brushPickerExpanded = false
                },
                onDismiss = { brushPickerExpanded = false },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StandardSlider(
            label = if (tool == DrawingTool.COLOR_FILL) "Tolerance" else "Size",
            value = config.size,
            onValueChange = {
                onIntent(TimelineIntent.Draw.SetBrushSize(it))
            },
            range = 1f..100f,
        )

        Spacer(modifier = Modifier.height(12.dp))

        StandardSlider(
            label = "Opacity",
            value = config.opacity,
            onValueChange = {
                onIntent(TimelineIntent.Draw.SetBrushOpacity(it))
            },
            range = 0.01f..1f,
            valueDisplay = { "${(it * 100).toInt()}%" },
        )

        if (tool != DrawingTool.COLOR_FILL && state.tool.constructionShape == null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolIconButton(Lucide.Minus, "Line", state.canvas.rulerMode == RulerMode.LINE) {
                    onIntent(TimelineIntent.View.SetRulerMode(RulerMode.LINE))
                }
                ToolIconButton(Lucide.Circle, "Circle", state.canvas.rulerMode == RulerMode.CIRCLE) {
                    onIntent(TimelineIntent.View.SetRulerMode(RulerMode.CIRCLE))
                }
                ToolIconButton(Lucide.Square, "Box", state.canvas.rulerMode == RulerMode.BOX) {
                    onIntent(TimelineIntent.View.SetRulerMode(RulerMode.BOX))
                }
                ToolIconButton(Lucide.FlipHorizontal, "Mirror", state.canvas.rulerMode == RulerMode.MIRROR) {
                    onIntent(TimelineIntent.View.SetRulerMode(RulerMode.MIRROR))
                }
                ToolIconButton(Lucide.X, "Off", state.canvas.rulerMode == RulerMode.NONE) {
                    onIntent(TimelineIntent.View.SetRulerMode(RulerMode.NONE))
                }
            }
        } else if (state.tool.constructionShape != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolIconButton(Lucide.Pill, "Capsule", state.tool.constructionShape == ConstructionShape.CAPSULE) {
                    onIntent(TimelineIntent.Draw.SelectConstructionShape(ConstructionShape.CAPSULE))
                }
                ToolIconButton(
                    Lucide.Triangle,
                    "Triangle",
                    state.tool.constructionShape == ConstructionShape.TRIANGLE,
                ) {
                    onIntent(TimelineIntent.Draw.SelectConstructionShape(ConstructionShape.TRIANGLE))
                }
                ToolIconButton(
                    Lucide.CirclePlus,
                    "Head guide",
                    state.tool.constructionShape == ConstructionShape.HEAD_GUIDE,
                ) {
                    onIntent(TimelineIntent.Draw.SelectConstructionShape(ConstructionShape.HEAD_GUIDE))
                }
                ToolIconButton(
                    Lucide.Spline,
                    "Three-point arc",
                    state.tool.constructionShape == ConstructionShape.THREE_POINT_ARC,
                ) {
                    onIntent(TimelineIntent.Draw.SelectConstructionShape(ConstructionShape.THREE_POINT_ARC))
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            icon,
            description,
            modifier = Modifier.size(18.dp),
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
        Text("Type", style = MaterialTheme.typography.labelMedium, color = EditorTokens.dark)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(Color(0xFFF7F5F8))
                .clickable(onClick = onClick)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(brushPreviewResource(brush)),
                contentDescription = "${brush.displayName} brush",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.size(10.dp))
            Text(brush.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            BrushCatalog.drawingBrushes.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(brushPreviewResource(option)),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.size(8.dp))
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
        BrushOptionsContent(TimelineState(), DrawingTool.BRUSH, {})
    }
}
