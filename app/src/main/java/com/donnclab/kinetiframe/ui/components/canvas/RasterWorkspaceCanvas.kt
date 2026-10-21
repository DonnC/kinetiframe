package com.donnclab.kinetiframe.ui.components.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.cache.FrameCacheManager
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import com.donnclab.kinetiframe.ui.currentFrameLayers

/** The FxF workspace: raster editing, selection, construction, ruler, and onion-skin surfaces. */
@Composable
fun RasterWorkspaceCanvas(
    state: TimelineState,
    onIntent: (TimelineIntent) -> Unit,
    cacheManager: FrameCacheManager?,
    /** The project's persistent background image, already decoded -- see [CanvasDrawingLayer]'s
     *  doc comment on its own parameter of the same name for the full rationale. */
    backgroundBitmap: android.graphics.Bitmap? = null,
    /** The video reference track's current-frame bitmap and its track-wide opacity -- see
     *  [CanvasDrawingLayer]'s doc comment on its own parameters of the same name. */
    videoFrameBitmap: android.graphics.Bitmap? = null,
    videoOpacity: Float = 0.5f,
    modifier: Modifier = Modifier,
    actorGeometry: com.donnclab.kinetiframe.domain.render.ActorSelectionGeometry? = null,
    externalSelectionOverlay: Boolean = false,
    paperDecoration: Boolean = true,
    sharedFontRegistry: com.donnclab.kinetiframe.engine.typography.FontRegistry? = null,
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
) {
    val config = state.tool.activeConfig
    Box(modifier = modifier.fillMaxSize()) {
        BufferedDrawingCanvas(
            referenceUnderlay = referenceUnderlay,
            timelineState = state,
            paperDecoration = paperDecoration,
            currentFrameBitmap = state.currentBitmap,
            backgroundBitmap = backgroundBitmap,
            videoFrameBitmap = videoFrameBitmap,
            videoOpacity = videoOpacity,
            onStrokeComplete = { points, brush -> onIntent(TimelineIntent.Draw.BakeStroke(points, brush)) },
            modifier = Modifier.fillMaxSize(),
            strokeColor = config.color.toArgb(),
            strokeWidth = config.size,
            strokeAlpha = config.opacity,
            strength = config.strength,
            brushId = config.brushId,
            activeTool = state.tool.activeTool,
            constructionShape = state.tool.constructionShape,
            constructionPreviewPending = state.constructionShape.previewActive,
            eraserType = config.eraserType,
            eraserCursor = state.tool.eraserCursor,
            selectionTool = state.selection.toolType,
            selectionActive = state.selection.active,
            selectionBounds = state.selection.bounds,
            selectionTransform = state.selection.transform,
            selectedContent = state.selection.selectedContent,
            externalSelectionOverlay = externalSelectionOverlay,
            sharedFontRegistry = sharedFontRegistry,
            onSelectionAreaComplete = { path, rect, density, registry ->
                onIntent(TimelineIntent.Selection.SelectArea(path, rect, density, registry, actorGeometry))
            },
            onSelectionTransformChanged = { onIntent(TimelineIntent.Selection.UpdateTransform(it)) },
            onEraserCursorChange = { offset -> onIntent(TimelineIntent.Draw.SetEraserCursor(offset)) },
            onionSkinEnabled = state.onionSkin.enabled && !state.animation.isPlaying,
            onionSkinFramesBefore = state.onionSkin.depthBefore,
            onionSkinFramesAfter = state.onionSkin.depthAfter,
            onionSkinColorBefore = state.onionSkin.colorBefore,
            onionSkinColorAfter = state.onionSkin.colorAfter,
            onionSkinOpacity = state.onionSkin.opacity,
            cacheManager = cacheManager,
            activeLayerId = state.currentFrameLayers().getOrNull(state.layers.activeLayerIndex)?.id ?: "main",
            nodes = state.scene.nodes,
            onTextNodeUpdate = { onIntent(TimelineIntent.Text.UpdateTextNode(it)) },
            onTextNodeSelect = { onIntent(TimelineIntent.Text.SelectTextNode(it)) },
            onIntent = onIntent,
            onConstructionPreview = { strokes -> onIntent(TimelineIntent.Construction.SetPreview(strokes)) },
        )

        if (state.constructionShape.previewActive) {
            ConstructionShapePreviewOverlay(
                strokes = state.constructionShape.previewStrokes,
                canvasMatrix = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) },
                color = config.color,
                width = config.size,
                alpha = config.opacity,
                brushId = config.brushId,
            )
        }

        state.canvas.rulerGuide?.let { guide ->
            if (state.tool.activeTool == DrawingTool.BRUSH) {
                RulerGuideOverlay(
                    guide = guide,
                    stage = state.project.stage,
                    canvasMatrix = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) },
                    canvasMatrixValues = state.canvas.matrixValues,
                    onGuideCommitted = { onIntent(TimelineIntent.View.UpdateRulerGuide(it)) },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun RasterWorkspaceCanvasPreview() {
    KinetiFrameTheme {
        RasterWorkspaceCanvas(
            state = TimelineState(),
            onIntent = {},
            cacheManager = null,
        )
    }
}
