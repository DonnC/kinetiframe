package com.donnclab.kinetiframe.ui.components.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.donnclab.kinetiframe.engine.drawing.ManipulationStrokeRenderer
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.models.Layer
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ManipulationLayerPreviewTest {
    @Test fun previewPreservesLowerLayersUpperLayersAndActiveOpacity() {
        val lower = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val source = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(source).drawCircle(28f, 32f, 12f, Paint().apply { color = Color.RED })
        val upper = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(upper).drawRect(32f, 0f, 40f, 64f, Paint().apply { color = Color.BLUE })
        val stroke = ActiveStroke()
        stroke.beginManipulation(source, DrawingTool.BLUR, 20f, 0.72f)
        stroke.appendSample(TouchPoint(25f, 32f, pressure = 1f, timestamp = 1))
        stroke.appendSample(TouchPoint(42f, 32f, pressure = 1f, timestamp = 17))
        val actual = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        ManipulationLayerPreview().draw(
            Canvas(actual),
            listOf(
                Layer("lower") to lower,
                Layer("active", opacity = 0.5f) to source,
                Layer("upper") to upper,
            ),
            "active",
            stroke,
            64,
            64,
        )
        val edited = source.copy(Bitmap.Config.ARGB_8888, true)
        ManipulationStrokeRenderer.render(Canvas(edited), edited, stroke.points, DrawingTool.BLUR, 20f, 0.72f)
        val expected = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(expected).apply {
            drawBitmap(lower, 0f, 0f, null)
            drawBitmap(edited, 0f, 0f, Paint().apply { alpha = 127 })
            drawBitmap(upper, 0f, 0f, null)
        }
        fun pixels(bitmap: Bitmap) = IntArray(64 * 64).also { bitmap.getPixels(it, 0, 64, 0, 0, 64, 64) }
        assertArrayEquals(pixels(expected), pixels(actual))
        stroke.reset()
        listOf(lower, source, upper, actual, edited, expected).forEach(Bitmap::recycle)
    }
}
