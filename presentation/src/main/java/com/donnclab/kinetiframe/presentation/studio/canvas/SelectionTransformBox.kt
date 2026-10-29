package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.donnclab.kinetiframe.core.document.TextNode
import com.donnclab.kinetiframe.studio.SelectedContent
import com.donnclab.kinetiframe.studio.SelectionInteractionMode
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.drawing.FontRegistry
import com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer

@Composable
fun SelectionTransformBox(
    selectedContent: SelectedContent?,
    selectionBounds: Rect,
    transformState: SelectionTransformState,
    fontRegistry: FontRegistry,
    displayDensity: Float,
    canvasMatrixValues: FloatArray,
    docWidth: Int,
    docHeight: Int,
    viewportWidth: Int,
    viewportHeight: Int,
    onOutsideSelectionTap: () -> Unit,
    onIntent: (StudioIntent) -> Unit,
    activeStroke: ActiveStroke?,
    modifier: Modifier = Modifier,
    onDragEnd: () -> Unit = {},
    lockAspectRatio: Boolean = false,
    skewMode: Boolean = false,
    interactionMode: SelectionInteractionMode = SelectionInteractionMode.TRANSFORM,
) {
    val gestureScale = remember(canvasMatrixValues) {
        kotlin.math.sqrt(
            (canvasMatrixValues[0] * canvasMatrixValues[0] + canvasMatrixValues[3] * canvasMatrixValues[3]).toDouble(),
        ).toFloat()
    }
    val viewport = remember(canvasMatrixValues) { android.graphics.Matrix().apply { setValues(canvasMatrixValues) } }
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        selectedContent?.let { content ->
            val proxyMatrix = remember { android.graphics.Matrix() }
            val values = remember { FloatArray(9) }
            val proxyPaint =
                remember {
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG,
                    )
                }
            val availableFonts = fontRegistry.availableFonts
            // Record editable text once per content/font change, not once per drag frame.
            val vectorProxy = remember(content.nodes, availableFonts, displayDensity, docWidth, docHeight) {
                Picture().apply {
                    val recording = beginRecording(docWidth, docHeight)
                    content.nodes.values.filterIsInstance<TextNode>().sortedBy { it.zIndex }.forEach { node ->
                        val typeface =
                            availableFonts.find { it.fileName == node.fontFileName }?.typeface
                                ?: Typeface.DEFAULT
                        MultilineTextRenderer.draw(recording, node, typeface, displayDensity)
                    }
                    endRecording()
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    val v = transformState.matrix.values
                    values[0] = v[0]
                    values[1] = v[4]
                    values[2] = v[12]
                    values[3] = v[1]
                    values[4] = v[5]
                    values[5] = v[13]
                    values[6] = 0f
                    values[7] = 0f
                    values[8] = 1f
                    proxyMatrix.setValues(values)
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.concat(viewport)
                    canvas.nativeCanvas.concat(proxyMatrix)
                    val proxy = content.bitmap
                    if (proxy != null) {
                        canvas.nativeCanvas.drawBitmap(proxy, content.bounds.left, content.bounds.top, proxyPaint)
                    }
                    canvas.nativeCanvas.drawPicture(vectorProxy)
                    canvas.nativeCanvas.restore()
                }
            }
        }

        SelectionTransformOverlay(
            bounds = selectionBounds,
            transformState = transformState,
            canvasMatrixValues = canvasMatrixValues,
            activeStroke = activeStroke,
            fitScale = 1f,
            gestureScale = gestureScale,
            onTapInside = { /* No-op, menu is persistent */ },
            onTapOutside = onOutsideSelectionTap,
            onDragEnd = onDragEnd,
            lockAspectRatio = lockAspectRatio,
            skewMode = skewMode,
            interactionMode = interactionMode,
        )
    }
}
