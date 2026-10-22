package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.nativeCanvas

/** Clip in inspection space, not moving-camera space. Matrices/paths are allocated outside draw. */
@Composable
internal fun Modifier.sceneCameraViewport(
    enabled: Boolean,
    viewport: FloatArray,
    width: Int,
    height: Int,
    black: Float,
): Modifier {
    val aperture = remember(viewport, width, height) {
        Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            transform(Matrix().apply { setValues(viewport) })
        }
    }
    val fade = remember(black) {
        Paint().apply {
            color = android.graphics.Color.BLACK
            alpha = (black * 255).toInt()
        }
    }
    return if (!enabled) {
        this
    } else {
        drawWithContent {
            val canvas = drawContext.canvas.nativeCanvas
            val checkpoint = canvas.save()
            try {
                canvas.clipPath(aperture)
                drawContent()
            } finally {
                canvas.restoreToCount(checkpoint)
            }
            if (black > 0) canvas.drawPath(aperture, fade)
        }
    }
}
