package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSaveLayer
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.BrushId
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawCommand
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer

@Composable
fun CanvasDrawingLayer(
    modifier: Modifier = Modifier,
    docWidth: Int,
    docHeight: Int,
    canvasMatrix: android.graphics.Matrix,
    /** The project's persistent background image, already decoded and cached by
     *  [com.donnclab.kinetiframe.studio.media.ProjectAssetManager] -- drawn once per frame, scaled to
     *  cover the sheet, behind onion skin and the current frame's raster content. Null means no
     *  background is set; the sheet's existing shadow/border fill shows through as before. */
    backgroundBitmap: Bitmap?,
    /** The video reference track's decoded frame for the current playhead position, if any video
     *  clip covers it -- see [com.donnclab.kinetiframe.studio.media.VideoFrameProvider]. Drawn on top of
     *  [backgroundBitmap] at [videoOpacity] so the artist can trace over it (a "rotoscope" aid);
     *  still well behind the current frame's own raster content and outside
     *  the eraser save-layer group, same as the background image above it in this file. */
    videoFrameBitmap: Bitmap? = null,
    videoOpacity: Float = 0.5f,
    onionSkinEnabled: Boolean,
    pastBitmaps: List<OnionFrame>,
    futureBitmaps: List<OnionFrame>,
    onionSkinOpacity: Float,
    pastTint: ColorFilter?,
    futureTint: ColorFilter?,
    currentFrameBitmap: Bitmap?,
    nodes: Map<String, com.donnclab.kinetiframe.core.document.BaseNode>,
    fontRegistry: FontRegistry,
    displayDensity: Float,
    activeStroke: ActiveStroke,
    activeTool: DrawingTool,
    eraserType: EraserType,
    strokeWidth: Float,
    strokeAlpha: Float = 1f,
    strength: Float = 0.5f,
    brush: BrushSnapshot = BrushSnapshot(),
    pressurePaint: android.graphics.Paint,
    selectionPath: androidx.compose.ui.graphics.Path,
    selectionPaint: android.graphics.Paint,
    selectionRect: Rect?,
    useSaveLayer: Boolean,
    fillPreviewBitmap: Bitmap? = null,
    fillPreviewActive: Boolean = false,
    manipulationLayers: List<Pair<Layer, Bitmap>> = emptyList(),
    activeLayerId: String = "",
    bakedStrokeSeeds: Set<Long> = emptySet(),
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
    paperDecoration: Boolean = true,
) {
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(20f, 0f, 10f, android.graphics.Color.argb(60, 0, 0, 0))
        }
    }
    val borderPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = 1f
        }
    }
    val paperPaint = remember { android.graphics.Paint().apply { color = android.graphics.Color.WHITE } }
    val rasterPaint = remember { android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG) }
    val androidMatrix = remember { android.graphics.Matrix() }
    val sheetRect =
        remember(docWidth, docHeight) { android.graphics.RectF(0f, 0f, docWidth.toFloat(), docHeight.toFloat()) }
    val manipulationRenderer = remember { ManipulationLayerPreview() }

    Box(modifier = modifier.fillMaxSize()) {
        // Static/Background Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Only tools that modify the displayed raster need pointer-rate redraws here.
            // Ordinary ink stays on the independent active-stroke canvas.
            if ((activeTool == DrawingTool.ERASER && eraserType == EraserType.AREA) ||
                activeTool == DrawingTool.BLUR || activeTool == DrawingTool.SMUDGE
            ) {
                activeStroke.drawTrigger
            }
            drawIntoCanvas { canvas ->
                // The sheet's white paper fill/shadow/border, and the persistent background image
                // on top of it (if one is set), are drawn here on their own -- straight to the
                // real canvas, before drawContent and well outside the eraser's withSaveLayer
                // group below -- so an area-eraser stroke, which clears its whole save-layer group
                // via PorterDuff.Mode.CLEAR, can never punch a hole through either of them. The
                // background is scaled to cover the sheet exactly (crop, not letterbox -- matches
                // FlipaClip's imported background) and deliberately not baked into
                // currentFrameBitmap or the frame cache: it isn't per-frame content, it's redrawn
                // identically every frame from one cached bitmap.
                canvas.save()
                canvas.nativeCanvas.concat(canvasMatrix)
                canvas.nativeCanvas.drawRect(sheetRect, if (paperDecoration) shadowPaint else paperPaint)
                if (paperDecoration) canvas.nativeCanvas.drawRect(sheetRect, borderPaint)
                referenceUnderlay?.let { draw ->
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.clipRect(sheetRect)
                    draw(canvas.nativeCanvas)
                    canvas.nativeCanvas.restore()
                }
                backgroundBitmap?.let { background ->
                    if (!background.isRecycled) {
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.clipRect(sheetRect)
                        val scale =
                            maxOf(docWidth.toFloat() / background.width, docHeight.toFloat() / background.height)
                        val drawWidth = background.width * scale
                        val drawHeight = background.height * scale
                        val left = (docWidth - drawWidth) / 2f
                        val top = (docHeight - drawHeight) / 2f
                        val destRect = android.graphics.RectF(left, top, left + drawWidth, top + drawHeight)
                        canvas.nativeCanvas.drawBitmap(background, null, destRect, null)
                        canvas.nativeCanvas.restore()
                    }
                }
                videoFrameBitmap?.let { video ->
                    if (!video.isRecycled) {
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.clipRect(sheetRect)
                        val scale = maxOf(docWidth.toFloat() / video.width, docHeight.toFloat() / video.height)
                        val drawWidth = video.width * scale
                        val drawHeight = video.height * scale
                        val left = (docWidth - drawWidth) / 2f
                        val top = (docHeight - drawHeight) / 2f
                        val destRect = android.graphics.RectF(left, top, left + drawWidth, top + drawHeight)
                        val videoPaint = android.graphics.Paint().apply {
                            alpha =
                                (videoOpacity.coerceIn(0f, 1f) * 255).toInt()
                        }
                        canvas.nativeCanvas.drawBitmap(video, null, destRect, videoPaint)
                        canvas.nativeCanvas.restore()
                    }
                }
                canvas.restore()

                val drawContent = {
                    canvas.save()
                    canvas.nativeCanvas.concat(canvasMatrix)

                    // Keep every document-owned layer inside the sheet.  The canvas
                    // transform may rotate or zoom the sheet, but clipping in document
                    // coordinates keeps the visible boundary and writable boundary in
                    // lockstep.
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.clipRect(sheetRect)

                    if (onionSkinEnabled) {
                        pastBitmaps.asReversed().forEach { frame ->
                            val alpha = (onionSkinOpacity / frame.distance).coerceIn(0f, 1f)
                            drawImage(frame.bitmap.asImageBitmap(), colorFilter = pastTint, alpha = alpha)
                        }
                        futureBitmaps.asReversed().forEach { frame ->
                            val alpha = (onionSkinOpacity / frame.distance).coerceIn(0f, 1f)
                            drawImage(frame.bitmap.asImageBitmap(), colorFilter = futureTint, alpha = alpha)
                        }
                    }

                    if (!manipulationRenderer.draw(
                            canvas.nativeCanvas,
                            manipulationLayers,
                            activeLayerId,
                            activeStroke,
                            docWidth,
                            docHeight,
                            bakedStrokeSeeds,
                        )
                    ) {
                        currentFrameBitmap?.takeUnless(Bitmap::isRecycled)?.let {
                            // Camera previews may prepare a bounded-resolution raster; stage
                            // coordinates remain unchanged for normal full-resolution editing.
                            canvas.nativeCanvas.drawBitmap(it, null, sheetRect, rasterPaint)
                        }
                    }

                    // Fill Tool Preview (Ghost View)
                    fillPreviewBitmap?.let { preview ->
                        if (fillPreviewActive) {
                            drawImage(
                                image = preview.asImageBitmap(),
                                alpha = 0.4f,
                                colorFilter = null,
                            )
                        }
                    }

                    nodes.values.filterIsInstance<TextNode>().forEach { node ->
                        val typeface =
                            fontRegistry.availableFonts.find { it.fileName == node.fontFileName }?.typeface
                                ?: android.graphics.Typeface.DEFAULT
                        MultilineTextRenderer.draw(canvas.nativeCanvas, node, typeface, displayDensity)
                    }
                    canvas.restore()
                    canvas.restore()
                }

                if (useSaveLayer) {
                    canvas.withSaveLayer(Rect(0f, 0f, size.width, size.height), Paint()) {
                        drawContent()
                        // Draw active eraser stroke inside the save layer so it can clear the background
                        if (activeTool == DrawingTool.ERASER && eraserType == EraserType.AREA &&
                            activeStroke.points.isNotEmpty()
                        ) {
                            canvas.save()
                            canvas.nativeCanvas.concat(canvasMatrix)
                            canvas.nativeCanvas.clipRect(sheetRect)
                            drawVariableWidthStroke(
                                canvas.nativeCanvas,
                                activeStroke,
                                strokeWidth,
                                pressurePaint,
                                activeTool,
                                brush,
                                currentFrameBitmap,
                                strength,
                            )
                            canvas.restore()
                        }

                        activeStroke.settledStrokes
                            .filterNot { it.brush.seed in bakedStrokeSeeds }
                            .filter { it.tool == DrawingTool.ERASER && it.eraserType == EraserType.AREA }
                            .forEach { settled ->
                                canvas.save()
                                canvas.nativeCanvas.concat(canvasMatrix)
                                canvas.nativeCanvas.clipRect(sheetRect)
                                drawVariableWidthStroke(
                                    canvas = canvas.nativeCanvas,
                                    points = settled.points,
                                    baseWidth = settled.width,
                                    paint = settled.paint,
                                    activeTool = settled.tool,
                                    brush = settled.brush,
                                    sourceBitmap = settled.sourceBitmap,
                                    strength = settled.strength,
                                )
                                canvas.restore()
                            }
                    }
                } else {
                    drawContent()
                }
            }
        }

        // Active Stroke Layer (120Hz isolated rendering)
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            activeStroke.drawTrigger // Re-draw only this layer when stroke updates

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.nativeCanvas.concat(canvasMatrix)
                canvas.nativeCanvas.clipRect(sheetRect)

                activeStroke.settledStrokes
                    .filterNot { it.brush.seed in bakedStrokeSeeds }
                    .filterNot { it.tool == DrawingTool.ERASER && it.eraserType == EraserType.AREA }
                    .filterNot { it.tool == DrawingTool.ERASER && it.eraserType == EraserType.STROKE }
                    .forEach { settled ->
                        val preview = settled.manipulationPreview
                        if (preview == null) {
                            drawVariableWidthStroke(
                                canvas = canvas.nativeCanvas,
                                points = settled.points,
                                baseWidth = settled.width,
                                paint = settled.paint,
                                activeTool = settled.tool,
                                brush = settled.brush,
                                sourceBitmap = settled.sourceBitmap,
                                strength = settled.strength,
                            )
                        }
                    }

                if (activeStroke.points.isNotEmpty()) {
                    // Only draw active stroke here if it's NOT an area eraser.
                    // Area erasers are drawn inside the background's saveLayer for real-time clearing.
                    if (activeTool != DrawingTool.ERASER || eraserType != EraserType.STROKE) {
                        val isAreaEraser = activeTool == DrawingTool.ERASER && eraserType == EraserType.AREA
                        if (!isAreaEraser) {
                            if (activeTool == DrawingTool.BRUSH && brush.id == BrushId.PEN) {
                                activeStroke.drawIncrementalPen(
                                    canvas = canvas.nativeCanvas,
                                    baseWidth = strokeWidth,
                                    paint = pressurePaint,
                                )
                            } else if (activeTool == DrawingTool.BLUR || activeTool == DrawingTool.SMUDGE) {
                                // Prepared by pointer events. Canvas only presents it; it never
                                // creates bitmaps or replays a manipulation stroke.
                                // Presented in the selected layer above, never as a scene overlay.
                            } else {
                                drawVariableWidthStroke(
                                    canvas = canvas.nativeCanvas,
                                    stroke = activeStroke,
                                    baseWidth = strokeWidth,
                                    paint = pressurePaint,
                                    activeTool = activeTool,
                                    brush = brush,
                                    sourceBitmap = currentFrameBitmap,
                                    strength = strength,
                                )
                            }
                        }
                    }
                }

                if (activeStroke.mirroredPoints.isNotEmpty()) {
                    drawVariableWidthStroke(
                        canvas = canvas.nativeCanvas,
                        points = activeStroke.mirroredPoints,
                        predictedPoint = activeStroke.predictedMirroredTouchPoint,
                        baseWidth = strokeWidth,
                        paint = pressurePaint,
                        activeTool = activeTool,
                        brush = brush,
                        sourceBitmap = currentFrameBitmap,
                        strength = strength,
                    )
                }

                if (activeTool == DrawingTool.LASSO) {
                    if (!selectionPath.isEmpty) {
                        canvas.nativeCanvas.drawPath(selectionPath.asAndroidPath(), selectionPaint)
                    }
                    selectionRect?.let { rect ->
                        canvas.nativeCanvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, selectionPaint)
                    }
                }
                canvas.restore()
            }
        }
    }
}
