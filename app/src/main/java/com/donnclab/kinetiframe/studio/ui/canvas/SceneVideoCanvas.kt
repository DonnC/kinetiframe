package com.donnclab.kinetiframe.studio.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.geometry.Affine2D
import com.donnclab.kinetiframe.video.SceneVideoRenderer

/** Pointer input belongs to the shared lasso surface, not to the video renderer. */
@Composable
internal fun SceneVideoCanvas(
    clip: VideoClipPlacement,
    bitmap: Bitmap,
    canvasValues: FloatArray,
    stageWidth: Int,
    stageHeight: Int,
    preview: () -> Affine2D,
    modifier: Modifier = Modifier,
    clipToStage: Boolean = true,
) {
    val renderer = remember { SceneVideoRenderer() }
    val matrix = remember { android.graphics.Matrix() }
    Canvas(modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            matrix.setValues(canvasValues)
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.concat(matrix)
            if (clipToStage) canvas.nativeCanvas.clipRect(0f, 0f, stageWidth.toFloat(), stageHeight.toFloat())
            renderer.draw(canvas.nativeCanvas, bitmap, clip, preview())
            canvas.nativeCanvas.restore()
        }
    }
}

@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun SceneVideoCanvasPreview() {
    val bitmap =
        remember {
            Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(32, 147, 154))
            }
        }
    val matrix = remember { android.graphics.Matrix().let { matrix -> FloatArray(9).also(matrix::getValues) } }
    SceneVideoCanvas(
        VideoClipPlacement(
            "preview", "video", "Video", 0, 12, isSceneContent = true,
            x = 40f, y = 30f, width = 160f, height = 90f,
        ),
        bitmap,
        matrix,
        320,
        180,
        { Affine2D() },
    )
}
