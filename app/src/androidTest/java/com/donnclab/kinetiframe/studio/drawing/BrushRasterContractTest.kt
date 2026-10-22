package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.donnclab.kinetiframe.project.model.TouchPoint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Pixel contracts for the material distinction that configuration-only tests cannot see. */
@RunWith(AndroidJUnit4::class)
class BrushRasterContractTest {

    @Test
    fun textured_recipes_replay_identically_for_the_same_snapshot() {
        val snapshot = BrushSnapshot(id = BrushId.CRAYON, seed = 991L)

        assertArrayEquals(render(snapshot), render(snapshot))
    }

    @Test
    fun textured_recipes_use_the_persisted_seed() {
        val first = render(BrushSnapshot(id = BrushId.CRAYON, seed = 991L))
        val second = render(BrushSnapshot(id = BrushId.CRAYON, seed = 992L))

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun paint_brush_has_a_soft_edge_outside_the_pen_silhouette() {
        val pen = render(BrushSnapshot(id = BrushId.PEN), width = 10f)
        val brush = render(BrushSnapshot(id = BrushId.PAINT), width = 10f)

        // The pen radius is 6 px at pressure 1. The paint brush's layered outer tip
        // intentionally reaches farther, leaving low-alpha pigment at this sample.
        assertEquals(0, Color.alpha(pixelAt(pen, 64, 73)))
        assertTrue(Color.alpha(pixelAt(brush, 64, 73)) > 0)
    }

    @Test
    fun highlighter_is_translucent_and_recipe_state_does_not_leak() {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val paint = basePaint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 10f
        }

        BrushRenderers.draw(
            Canvas(bitmap),
            stroke,
            width = 10f,
            paint = paint,
            snapshot = BrushSnapshot(id = BrushId.HIGHLIGHTER),
        )

        assertTrue(Color.alpha(pixelAt(bitmap, 64, 64)) in 1..200)
        assertEquals(Paint.Style.STROKE, paint.style)
        assertEquals(Paint.Cap.ROUND, paint.strokeCap)
        assertEquals(Paint.Join.ROUND, paint.strokeJoin)
        assertEquals(10f, paint.strokeWidth, 0f)
        assertEquals(255, paint.alpha)
        assertEquals(null, paint.xfermode)
    }

    @Test
    fun highlighter_is_a_non_destructive_translucent_overlay() {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        BrushRenderers.draw(
            canvas,
            stroke,
            10f,
            basePaint().apply {
                color = Color.BLACK
            },
            BrushSnapshot(id = BrushId.PEN),
        )
        val inkBeforeHighlight = pixelAt(bitmap, 64, 64)
        BrushRenderers.draw(
            canvas,
            stroke,
            10f,
            basePaint().apply {
                color = Color.YELLOW
            },
            BrushSnapshot(id = BrushId.HIGHLIGHTER),
        )

        val result = pixelAt(bitmap, 64, 64)
        assertEquals(255, Color.alpha(result))
        assertTrue(Color.red(result) > Color.red(inkBeforeHighlight))
        assertTrue(Color.green(result) > Color.green(inkBeforeHighlight))
        assertTrue(Color.blue(result) < 16)
    }

    @Test
    fun airbrush_has_a_gradient_falloff_not_a_hard_ring() {
        val bitmap = renderBitmap(BrushSnapshot(id = BrushId.AIRBRUSH), width = 10f)

        assertTrue(Color.alpha(pixelAt(bitmap, 64, 64)) > Color.alpha(pixelAt(bitmap, 64, 72)))
        assertTrue(Color.alpha(pixelAt(bitmap, 64, 72)) > 0)
    }

    @Test
    fun phase_two_brushes_emit_deterministic_marks() {
        val ids = listOf(
            BrushId.HAIR, BrushId.FUR, BrushId.SKETCH, BrushId.CALLIGRAPHY,
            BrushId.SPRAY, BrushId.STIPPLE, BrushId.ROUGH_PEN, BrushId.GRAIN_PEN,
            BrushId.DASH, BrushId.GEAR, BrushId.SAW,
        )
        ids.forEach { id ->
            val snapshot = BrushSnapshot(id = id, seed = 420L)
            val first = render(snapshot)
            val replay = render(snapshot)
            assertTrue("$id should leave pixels", first.any { Color.alpha(it) > 0 })
            assertArrayEquals("$id must replay identically", first, replay)
        }
    }

    @Test
    fun dash_brush_preserves_visible_gaps() {
        val pixels = render(BrushSnapshot(id = BrushId.DASH), width = 6f)
        val centerLine = (0 until SIZE).count { x -> Color.alpha(pixelAt(pixels, x, 64)) == 0 }
        assertTrue("dash pattern should contain transparent gaps", centerLine > SIZE / 4)
    }

    private fun render(snapshot: BrushSnapshot, width: Float = 10f): IntArray =
        renderBitmap(snapshot, width).copyPixels()

    private fun renderBitmap(snapshot: BrushSnapshot, width: Float = 10f): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        BrushRenderers.draw(Canvas(bitmap), stroke, width, basePaint(), snapshot)
        return bitmap
    }

    private fun pixelAt(pixels: IntArray, x: Int, y: Int): Int = pixels[y * SIZE + x]

    private fun pixelAt(bitmap: Bitmap, x: Int, y: Int): Int = bitmap.getPixel(x, y)

    private fun Bitmap.copyPixels(): IntArray = IntArray(SIZE * SIZE).also {
        getPixels(it, 0, SIZE, 0, 0, SIZE, SIZE)
    }

    private fun basePaint() = Paint().apply {
        color = Color.BLACK
        alpha = 255
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private companion object {
        const val SIZE = 128
        val stroke = listOf(
            TouchPoint(24f, 64f, pressure = 1f),
            TouchPoint(104f, 64f, pressure = 1f),
        )
    }
}
