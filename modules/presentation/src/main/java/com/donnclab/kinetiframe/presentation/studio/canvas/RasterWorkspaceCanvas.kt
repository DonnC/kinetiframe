package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.common.toAffine2D
import com.donnclab.kinetiframe.presentation.common.toMatrix
import com.donnclab.kinetiframe.presentation.common.toOffset
import com.donnclab.kinetiframe.presentation.common.toPoint2
import com.donnclab.kinetiframe.presentation.common.toRect
import com.donnclab.kinetiframe.presentation.common.toRect2
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.currentFrameLayers
import com.donnclab.kinetiframe.studio.render.FrameCacheManager

/** The FxF workspace: raster editing, selection, construction, ruler, and onion-skin surfaces. */
@Composable
fun RasterWorkspaceCanvas(
    state: StudioState,
    onIntent: (StudioIntent) -> Unit,
    cacheManager: FrameCacheManager?,
    modifier: Modifier = Modifier,
    /** The project's persistent background image, already decoded -- see [CanvasDrawingLayer]'s
     *  doc comment on its own parameter of the same name for the full rationale. */
    backgroundBitmap: android.graphics.Bitmap? = null,
    /** The video reference track's current-frame bitmap and its track-wide opacity -- see
     *  [CanvasDrawingLayer]'s doc comment on its own parameters of the same name. */
    videoFrameBitmap: android.graphics.Bitmap? = null,
    videoOpacity: Float = 0.5f,
    actorGeometry: com.donnclab.kinetiframe.actors.render.ActorSelectionGeometry? = null,
    externalSelectionOverlay: Boolean = false,
    paperDecoration: Boolean = true,
    sharedFontRegistry: com.donnclab.kinetiframe.studio.drawing.FontRegistry? = null,
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
) {
    val config = state.tool.activeConfig
    // A Compose Matrix compares by identity: convert once per transform so effects keyed on it stay put.
    val selectionMatrix = remember(state.selection.transform) { state.selection.transform.toMatrix() }
    val selectionBounds = remember(state.selection.bounds) { state.selection.bounds?.toRect() }
    Box(modifier = modifier.fillMaxSize()) {
        BufferedDrawingCanvas(
            referenceUnderlay = referenceUnderlay,
            studioState = state,
            paperDecoration = paperDecoration,
            currentFrameBitmap = state.currentBitmap,
            backgroundBitmap = backgroundBitmap,
            videoFrameBitmap = videoFrameBitmap,
            videoOpacity = videoOpacity,
            onStrokeComplete = { points, brush -> onIntent(StudioIntent.Draw.BakeStroke(points, brush)) },
            modifier = Modifier.fillMaxSize(),
            strokeColor = config.color,
            strokeWidth = config.size,
            strokeAlpha = config.opacity,
            strength = config.strength,
            brushId = config.brushId,
            activeTool = state.tool.activeTool,
            constructionShape = state.tool.constructionShape,
            constructionPreviewPending = state.constructionShape.previewActive,
            eraserType = config.eraserType,
            eraserCursor = state.tool.eraserCursor?.toOffset(),
            selectionTool = state.selection.toolType,
            selectionActive = state.selection.active,
            selectionBounds = selectionBounds,
            selectionTransform = selectionMatrix,
            selectedContent = state.selection.selectedContent,
            externalSelectionOverlay = externalSelectionOverlay,
            sharedFontRegistry = sharedFontRegistry,
            onSelectionAreaComplete = { path, rect, density, registry ->
                onIntent(
                    StudioIntent.Selection.SelectArea(
                        path?.asAndroidPath(),
                        rect?.toRect2(),
                        density,
                        registry,
                        actorGeometry,
                    ),
                )
            },
            onSelectionTransformChanged = { onIntent(StudioIntent.Selection.UpdateTransform(it.toAffine2D())) },
            onEraserCursorChange = { offset -> onIntent(StudioIntent.Draw.SetEraserCursor(offset?.toPoint2())) },
            onionSkinEnabled = state.onionSkin.enabled && !state.animation.isPlaying,
            onionSkinFramesBefore = state.onionSkin.depthBefore,
            onionSkinActiveLayerOnly = state.onionSkin.activeLayerOnly,
            onionSkinFramesAfter = state.onionSkin.depthAfter,
            onionSkinColorBefore = Color(state.onionSkin.colorBefore),
            onionSkinColorAfter = Color(state.onionSkin.colorAfter),
            onionSkinOpacity = state.onionSkin.opacity,
            cacheManager = cacheManager,
            activeLayerId = state.currentFrameLayers().getOrNull(state.layers.activeLayerIndex)?.id ?: "main",
            nodes = state.scene.nodes,
            onTextNodeUpdate = { onIntent(StudioIntent.Text.UpdateTextNode(it)) },
            onTextNodeSelect = { onIntent(StudioIntent.Text.SelectTextNode(it)) },
            onIntent = onIntent,
            onConstructionPreview = { strokes -> onIntent(StudioIntent.Construction.SetPreview(strokes)) },
        )

        if (state.constructionShape.previewActive) {
            ConstructionShapePreviewOverlay(
                strokes = state.constructionShape.previewStrokes,
                canvasMatrix = android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) },
                color = Color(config.color),
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
                    onGuideCommitted = { onIntent(StudioIntent.View.UpdateRulerGuide(it)) },
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
            state = StudioState(),
            onIntent = {},
            cacheManager = null,
        )
    }
}
