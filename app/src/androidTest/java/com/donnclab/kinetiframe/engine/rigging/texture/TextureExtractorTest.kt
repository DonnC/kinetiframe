package com.donnclab.kinetiframe.engine.rigging.texture

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextureExtractorTest {
    @Test
    fun lassoExtractionReturnsTightCrop() {
        val source = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val lasso = Path().apply {
            moveTo(1000f, 100f)
            lineTo(1100f, 100f)
            lineTo(1100f, 200f)
            lineTo(1000f, 200f)
            close()
        }

        val result = TextureExtractor.extractLassoCrop(source, lasso)

        assertEquals(100, result.width)
        assertEquals(100, result.height)
        result.recycle()
        source.recycle()
    }

    @Test
    fun lassoExtractionMasksPixelsOutsideTheClosedPath() {
        val source = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.TRANSPARENT)
            setPixel(3, 3, android.graphics.Color.RED)
            setPixel(10, 10, android.graphics.Color.BLUE)
        }
        val lasso = Path().apply {
            moveTo(2f, 2f)
            lineTo(7f, 2f)
            lineTo(2f, 7f)
            close()
        }

        val result = TextureExtractor.extractLassoCrop(source, lasso)

        assertEquals(5, result.width)
        assertEquals(5, result.height)
        assertEquals(android.graphics.Color.RED, result.getPixel(1, 1))
        assertEquals(0, result.getPixel(4, 4) ushr 24)
        result.recycle()
        source.recycle()
    }

    @Test
    fun lassoExtractionClampsAnEdgeSelectionToTheSourceBitmap() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val lasso = Path().apply {
            moveTo(-3f, 7f)
            lineTo(4f, 7f)
            lineTo(4f, 14f)
            lineTo(-3f, 14f)
            close()
        }

        val result = TextureExtractor.extractLassoCrop(source, lasso)

        assertEquals(4, result.width)
        assertEquals(3, result.height)
        assertTrue(result.allPixelsTransparent())
        result.recycle()
        source.recycle()
    }

    private fun Bitmap.allPixelsTransparent(): Boolean =
        IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
            .all { pixel -> pixel ushr 24 == 0 }
}
