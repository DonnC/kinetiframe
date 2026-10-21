package com.donnclab.kinetiframe.ui.components.canvas

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.engine.drawing.BrushId
import com.donnclab.kinetiframe.engine.drawing.BrushRenderers
import com.donnclab.kinetiframe.engine.drawing.BrushSnapshot
import com.donnclab.kinetiframe.engine.model.TouchPoint

/** Non-destructive construction-shape ghost. Permanent pixels are created only on Confirm. */
@Composable
fun ConstructionShapePreviewOverlay(
    strokes: List<List<TouchPoint>>,
    canvasMatrix: Matrix,
    color: Color,
    width: Float,
    alpha: Float,
    brushId: BrushId = BrushId.PEN,
) {
    if (strokes.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { target ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.copy(alpha = alpha * 0.65f).toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = width
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            val canvas = target.nativeCanvas
            canvas.save()
            canvas.concat(canvasMatrix)
            strokes.forEach { stroke ->
                BrushRenderers.draw(
                    canvas,
                    stroke,
                    width,
                    paint,
                    BrushSnapshot(
                        id = brushId,
                        seed =
                        stroke.firstOrNull()?.timestamp ?: 0L,
                    ),
                )
            }
            canvas.restore()
        }
    }
}
