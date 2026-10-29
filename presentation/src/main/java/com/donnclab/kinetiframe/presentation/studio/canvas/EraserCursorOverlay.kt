package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun EraserCursorOverlay(cursor: Offset, strokeWidth: Float, fitScale: Float, modifier: Modifier = Modifier) {
    val cursorRadius = (strokeWidth * fitScale) / 2f

    Canvas(modifier.fillMaxSize()) {
        // Outer ring
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = cursorRadius,
            center = cursor,
            style = Stroke(width = 3f),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = cursorRadius,
            center = cursor,
            style = Stroke(width = 1f),
        )

        // Center dot
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = cursor,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 1.dp.toPx(),
            center = cursor,
        )
    }
}
