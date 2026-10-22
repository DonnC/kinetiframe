package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.project.model.SafeFrameRatio
import com.donnclab.kinetiframe.project.model.safeFrame
import com.donnclab.kinetiframe.studio.StudioState

/** Non-interactive document-space stage and export-safe guides. */
@Composable
fun StageGuideOverlay(state: StudioState, modifier: Modifier = Modifier) {
    // The paper renderer already supplies a neutral edge. Do not draw a second,
    // coloured stage boundary (which incorrectly looked like a selection outline).
    if (!state.canvas.showSafeFrame) return
    val guideColor = MaterialTheme.colorScheme.tertiary
    val matrix =
        remember(state.canvas.matrixValues) { android.graphics.Matrix().apply { setValues(state.canvas.matrixValues) } }
    val paint = remember(guideColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = guideColor.copy(alpha = .55f).toArgb()
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(16f, 12f), 0f)
        }
    }
    val frame = remember(state.project.stage) { state.project.stage.safeFrame(SafeFrameRatio.WIDESCREEN_16_9) }
    Canvas(modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.concat(matrix)
            canvas.nativeCanvas.drawRect(frame.left, frame.top, frame.right, frame.bottom, paint)
            canvas.nativeCanvas.restore()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StageGuideOverlayPreview() = KinetiFrameTheme { StageGuideOverlay(StudioState()) }
