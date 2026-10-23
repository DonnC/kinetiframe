package com.donnclab.kinetiframe.studio.ui.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Droplets
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.FlipHorizontal
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Lasso
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Pen
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.X
import com.donnclab.kinetiframe.common.ui.StandardSlider
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.CanvasState
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.actorDefinition
import kotlin.math.roundToInt

/**
 * A draggable and scrollable tool dock.
 *
 * The dock position is screen-space, rather than edge-relative. Only the grip
 * starts a move so tool taps and tool context menus remain independent.
 */
@Composable
fun ToolDock(
    modifier: Modifier = Modifier,
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    onInteraction: () -> Unit = {},
) {
    // A pending extraction owns the canvas and its Check/Cancel controls exclusively.
    if (state.scene.pivotPlacement != null) return
    var menuTool by remember { mutableStateOf<DrawingTool?>(null) }
    val dockState = rememberDraggableDockState(state.canvas.dockOffset)
    var isDragging by remember { mutableStateOf(false) }

    val isPuppetStudio = state.scene.studioMode != StudioMode.RASTER_DRAWING
    val isPosing = state.scene.studioMode == StudioMode.ANIMATE_IK
    val performanceOnly = state.actor.isPerformanceAuthoring

    LaunchedEffect(state.canvas.dockOffset, dockState.containerSize, dockState.surfaceSize) {
        if (!isDragging) dockState.snapTo(state.canvas.dockOffset)
    }

    LaunchedEffect(state.tool.activeTool) {
        if (menuTool != null && state.tool.activeTool != menuTool) {
            menuTool = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .dockContainer(dockState),
    ) {
        if (state.fill.previewActive) {
            FillDock(state = state, onIntent = onIntent)
        }
        if (state.constructionShape.previewActive) {
            ConstructionShapeDock(state = state, onIntent = onIntent)
        }

        Surface(
            modifier = Modifier.dockSurface(dockState),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DragHandle(
                    modifier = Modifier.dockDragHandle(
                        dockState,
                        onDragStart = { isDragging = true },
                        onDragCancel = {
                            isDragging = false
                            dockState.snapTo(state.canvas.dockOffset)
                        },
                        onDragEnd = { finalOffset ->
                            isDragging = false
                            onIntent(StudioIntent.View.SetDockPosition(state.canvas.dockEdge, finalOffset))
                        },
                    ),
                )

                LazyColumn(
                    modifier = Modifier
                        .width(48.dp)
                        .height(
                            if (isPuppetStudio) {
                                300.dp
                            } else if (state.actorDefinition() !=
                                null
                            ) {
                                220.dp
                            } else {
                                180.dp
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    // Bone sits beside Pen for any document with a rig -- creating an actor
                    // ("Puppet layer") only ever happens inside the standalone Actor Studio (see
                    // LayerPanel), so this can never surface in a plain FxF project.
                    if (!performanceOnly && state.actorDefinition() != null) {
                        item {
                            ToolButton(
                                Lucide.Bone,
                                "Bone",
                                state.scene.studioMode == StudioMode.RIG_SETUP,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RIG_SETUP))
                                },
                                onOpenMenu = {},
                            )
                        }
                    }
                    if (!isPosing) {
                        item {
                            ToolButton(
                                Lucide.Pen,
                                "Pen",
                                !isPuppetStudio && state.tool.activeTool == DrawingTool.BRUSH &&
                                    state.tool.constructionShape == null,
                                onClick = {
                                    onInteraction()
                                    if (isPuppetStudio) {
                                        onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING))
                                    } else {
                                        onIntent(StudioIntent.Draw.SelectTool(DrawingTool.BRUSH))
                                    }
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    if (isPuppetStudio) {
                                        onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.RASTER_DRAWING))
                                    } else {
                                        onIntent(StudioIntent.Draw.SelectTool(DrawingTool.BRUSH))
                                        menuTool = DrawingTool.BRUSH
                                    }
                                },
                            )
                        }
                    }
                    if (isPuppetStudio) {
                        if (!isPosing) {
                            item {
                                ToolButton(
                                    Lucide.Plus,
                                    "Add Bone",
                                    state.tool.activeTool == DrawingTool.BONE,
                                    onClick = {
                                        onInteraction()
                                        onIntent(StudioIntent.Draw.SelectTool(DrawingTool.BONE))
                                    },
                                    onOpenMenu = {},
                                )
                            }
                        }
                        item {
                            ToolButton(
                                Lucide.Shapes,
                                "Pose Limb",
                                isPosing,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Rigging.SetStudioMode(StudioMode.ANIMATE_IK))
                                },
                                onOpenMenu = {},
                            )
                        }
                        item { HorizontalDivider(Modifier.width(32.dp).padding(vertical = 4.dp)) }
                    }
                    if (!isPuppetStudio) {
                        item {
                            ToolButton(
                                Lucide.Shapes,
                                "Construction shapes",
                                state.tool.constructionShape != null,
                                onClick = {
                                    onInteraction()
                                    onIntent(
                                        StudioIntent.Draw.SelectConstructionShape(
                                            state.tool.constructionShape
                                                ?: com.donnclab.kinetiframe.studio.drawing.ConstructionShape.CAPSULE,
                                        ),
                                    )
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    menuTool = DrawingTool.BRUSH
                                },
                            )
                        }
                    }
                    if (!isPuppetStudio) {
                        item {
                            ToolButton(
                                Lucide.Eraser,
                                "Eraser",
                                state.tool.activeTool == DrawingTool.ERASER,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.ERASER))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.ERASER))
                                    menuTool = DrawingTool.ERASER
                                },
                            )
                        }
                    }

                    if (isPuppetStudio) {
                        if (!isPosing) {
                            item {
                                ToolButton(
                                    Lucide.Lasso,
                                    "Select actor part",
                                    state.tool.activeTool == DrawingTool.LASSO,
                                    onClick = {
                                        onInteraction()
                                        onIntent(StudioIntent.Draw.SelectTool(DrawingTool.LASSO))
                                    },
                                    onOpenMenu = {},
                                )
                            }
                        }
                    } else if (!isPosing) {
                        item {
                            ToolButton(
                                Lucide.Lasso,
                                "Lasso",
                                state.tool.activeTool == DrawingTool.LASSO,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.LASSO))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.LASSO))
                                    menuTool = DrawingTool.LASSO
                                },
                            )
                        }
                    }

                    if (!isPuppetStudio) {
                        item {
                            ToolButton(
                                Lucide.PaintBucket,
                                "Fill",
                                state.tool.activeTool == DrawingTool.COLOR_FILL,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.COLOR_FILL))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.COLOR_FILL))
                                    menuTool = DrawingTool.COLOR_FILL
                                },
                            )
                        }
                    }

                    if (!isPuppetStudio) {
                        item {
                            ToolButton(
                                Lucide.Type,
                                "Text",
                                state.tool.activeTool == DrawingTool.TEXT,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.TEXT))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.TEXT))
                                    menuTool = DrawingTool.TEXT
                                },
                            )
                        }
                        item {
                            ToolButton(
                                Lucide.Hand,
                                "Smudge",
                                state.tool.activeTool == DrawingTool.SMUDGE,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.SMUDGE))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.SMUDGE))
                                    menuTool = DrawingTool.SMUDGE
                                },
                            )
                        }
                        item {
                            ToolButton(
                                Lucide.Droplets,
                                "Blur",
                                state.tool.activeTool == DrawingTool.BLUR,
                                onClick = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.BLUR))
                                },
                                onOpenMenu = {
                                    onInteraction()
                                    onIntent(StudioIntent.Draw.SelectTool(DrawingTool.BLUR))
                                    menuTool = DrawingTool.BLUR
                                },
                            )
                        }
                    }
                }
            }
        }

        if (menuTool != null) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    (dockState.offset.x + with(density) { 52.dp.toPx() }).roundToInt(),
                    dockState.offset.y.roundToInt(),
                ),
                onDismissRequest = { menuTool = null },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier
                        .width(if (menuTool == DrawingTool.GRID) 240.dp else 200.dp)
                        .padding(vertical = 8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                ) {
                    when (menuTool) {
                        DrawingTool.BRUSH, DrawingTool.COLOR_FILL -> BrushOptionsContent(state, menuTool!!, onIntent)
                        DrawingTool.ERASER -> EraserOptionsContent(state, onIntent) { menuTool = null }
                        DrawingTool.LASSO -> SelectionOptionsContent(state, onIntent) { menuTool = null }
                        DrawingTool.TEXT -> TypographyOptionsPanel(state, onIntent, onDismiss = { menuTool = null })
                        DrawingTool.SMUDGE, DrawingTool.BLUR -> SmudgeBlurOptionsContent(state, menuTool!!, onIntent)
                        else -> Box(Modifier.padding(16.dp)) { Text("Options") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun ToolDockPreview() {
    Box(Modifier.fillMaxSize()) {
        ToolDock(
            state = StudioState(
                canvas = CanvasState(dockOffset = Offset(20f, 100f)),
            ),
            onIntent = {},
        )
    }
}
