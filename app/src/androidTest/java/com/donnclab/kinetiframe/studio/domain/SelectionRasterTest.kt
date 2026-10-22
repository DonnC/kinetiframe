package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectionRasterTest {
    @Test
    fun rectangleExtractsAndClearsOnlyCoveredPixels() {
        val source = twoMarks()
        val rect = Rect(1f, 1f, 5f, 5f)

        val cut = requireNotNull(SelectionRaster.extract(source, null, rect))
        SelectionRaster.clear(source, null, rect)

        assertEquals(Rect(2f, 2f, 4f, 4f), cut.bounds)
        assertEquals(Color.RED, cut.bitmap.getPixel(0, 0))
        assertEquals(Color.TRANSPARENT, source.getPixel(2, 2))
        assertEquals("outside content must remain in the background", Color.BLUE, source.getPixel(10, 2))
    }

    @Test
    fun lassoDoesNotCaptureContentOutsideItsClosedPath() {
        val source = twoMarks()
        val lasso = Path().apply {
            moveTo(0f, 0f)
            lineTo(6f, 0f)
            lineTo(0f, 6f)
            close()
        }

        val cut = SelectionRaster.extract(source, lasso, null)
        SelectionRaster.clear(source, lasso, null)

        assertNotNull(cut)
        assertEquals(Color.TRANSPARENT, source.getPixel(2, 2))
        assertEquals(Color.BLUE, source.getPixel(10, 2))
    }

    @Test
    fun emptyCoverageProducesNoSelection() {
        assertNull(SelectionRaster.extract(twoMarks(), null, Rect(20f, 20f, 24f, 24f)))
    }

    private fun twoMarks(): Bitmap = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { style = Paint.Style.FILL }
        paint.color = Color.RED
        canvas.drawRect(2f, 2f, 4f, 4f, paint)
        paint.color = Color.BLUE
        canvas.drawRect(10f, 2f, 12f, 4f, paint)
    }
}
