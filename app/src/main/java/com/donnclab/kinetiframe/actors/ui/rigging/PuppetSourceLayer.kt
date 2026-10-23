package com.donnclab.kinetiframe.actors.ui.rigging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState
import com.donnclab.kinetiframe.studio.showActorSourceArtwork
import com.donnclab.kinetiframe.studio.ui.canvas.ActiveStroke
import com.donnclab.kinetiframe.studio.ui.canvas.drawVariableWidthStroke

/** Source artwork is a reference for extraction; confirmed parts remain separate assets. */
@Composable
fun PuppetSourceLayer(
    state: StudioState,
    path: Path,
    stroke: ActiveStroke,
    brush: BrushSnapshot = BrushSnapshot(),
    baseWidth: Float = 5f,
    strength: Float = 0.5f,
    referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
    drawSource: Boolean = true,
    drawMarks: Boolean = true,
) {
    val matrix = remember(state.canvas.matrixValues) {
        android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) }
    }
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    val paint = remember(accent) {
        android.graphics.Paint(3).apply {
            color = accent
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = 2f
        }
    }

    val pressurePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
    }

    // Rig Fabricator's "Pivot Switch": while adjusting joints the sketch is a dimmed reference
    // underneath the fully-interactive skeleton overlay, same as an onion-skin layer, so dragged
    // pins read clearly against it. Full opacity again once back to the Pen tool (RASTER_DRAWING)
    // -- and, per Performance Stage's own spec, full opacity there too, since ANIMATE_IK renders
    // the finished artwork skin at 100% with only the pose handles overlaid on top.
    val sourcePaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }

    val strokeColor = state.tool.activeConfig.color.toArgb()
    val strokeAlpha = state.tool.activeConfig.opacity

    SideEffect {
        pressurePaint.color = strokeColor
        pressurePaint.alpha = (strokeAlpha * 255).toInt()
        pressurePaint.strokeWidth = baseWidth
        sourcePaint.alpha = if (state.scene.studioMode == StudioMode.RIG_SETUP) 102 else 255
    }

    Canvas(Modifier.fillMaxSize()) {
        stroke.drawTrigger // The lasso path is mutable; input explicitly invalidates its preview.
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(matrix)
            native.clipRect(0f, 0f, state.project.stage.width.toFloat(), state.project.stage.height.toFloat())
            if (drawSource) {
                native.drawColor(android.graphics.Color.WHITE)
                referenceUnderlay?.invoke(native)
                if (state.showActorSourceArtwork() &&
                    state.tool.activeTool != com.donnclab.kinetiframe.core.document.DrawingTool.ERASER
                ) {
                    state.currentBitmap?.takeUnless {
                        it.isRecycled
                    }?.let { native.drawBitmap(it, 0f, 0f, sourcePaint) }
                }
            }
            if (drawMarks) {
                // Draw any settled strokes not yet baked into the bitmap (prevent blink)
                stroke.settledStrokes.filterNot {
                    it.brush.seed in state.bakedStrokeSeeds ||
                        it.tool == com.donnclab.kinetiframe.core.document.DrawingTool.ERASER
                }.forEach { settled ->
                    settled.manipulationPreview?.takeUnless { it.bitmap.isRecycled }?.let { preview ->
                        native.drawBitmap(preview.bitmap, preview.left.toFloat(), preview.top.toFloat(), null)
                    }
                        ?: drawVariableWidthStroke(
                            canvas = native,
                            points = settled.points,
                            baseWidth = settled.width,
                            paint = settled.paint,
                            activeTool = settled.tool,
                            brush = settled.brush,
                            sourceBitmap = settled.sourceBitmap,
                            strength = settled.strength,
                        )
                }

                // Draw active stroke
                if (stroke.points.isNotEmpty() &&
                    state.tool.activeTool != com.donnclab.kinetiframe.core.document.DrawingTool.ERASER
                ) {
                    if (state.tool.activeTool == com.donnclab.kinetiframe.core.document.DrawingTool.BLUR ||
                        state.tool.activeTool == com.donnclab.kinetiframe.core.document.DrawingTool.SMUDGE
                    ) {
                        stroke.manipulationPreview()?.takeUnless { it.bitmap.isRecycled }?.let { preview ->
                            native.drawBitmap(preview.bitmap, preview.left.toFloat(), preview.top.toFloat(), null)
                        }
                    } else if (state.tool.activeTool == com.donnclab.kinetiframe.core.document.DrawingTool.BRUSH &&
                        brush.id == com.donnclab.kinetiframe.core.document.BrushId.PEN
                    ) {
                        stroke.drawIncrementalPen(native, baseWidth, pressurePaint)
                    } else {
                        drawVariableWidthStroke(
                            canvas = native,
                            stroke = stroke,
                            baseWidth = baseWidth,
                            paint = pressurePaint,
                            activeTool = state.tool.activeTool,
                            brush = brush,
                            sourceBitmap = state.currentBitmap,
                            strength = strength,
                        )
                    }
                }

                native.drawPath(path.asAndroidPath(), paint)
            }
            native.restore()
        }
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun PuppetSourcePreview() {
    PuppetSourceLayer(StudioState(), remember { Path() }, remember { ActiveStroke() })
}
