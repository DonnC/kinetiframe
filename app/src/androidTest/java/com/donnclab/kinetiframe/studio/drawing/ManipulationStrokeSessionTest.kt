package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.TouchPoint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Pixel contracts for the shared live-preview/persisted manipulation renderer. */
@RunWith(AndroidJUnit4::class)
class ManipulationStrokeSessionTest {

    @Test fun blur_preview_pixels_match_commit_including_transparent_edges() {
        val source = sourceBitmap()
        val points = stroke()
        val committed = source.copy(Bitmap.Config.ARGB_8888, true)
        ManipulationStrokeRenderer.render(Canvas(committed), committed, points, DrawingTool.BLUR, 20f, 0.72f)
        val session = ManipulationStrokeSession(source, DrawingTool.BLUR, 20f, 0.72f)
        points.indices.forEach { session.append(points.subList(0, it + 1)) }
        session.finish(points)
        val result = session.copyResult()
        assertArrayEquals(pixels(committed), pixels(result))
        val preview = requireNotNull(session.preview())
        // What is displayed is now the modified layer itself, not a second independently
        // evaluated overlay that can diverge from the saved stroke.
        assertEquals(result.getPixel(preview.left + 1, preview.top + 1), preview.bitmap.getPixel(1, 1))
        session.release()
        session.releasePreview()
        result.recycle()
        committed.recycle()
        source.recycle()
    }

    @Test
    fun smudge_session_matches_the_persisted_renderer() {
        val source = sourceBitmap()
        val points = stroke()
        val committed = source.copy(Bitmap.Config.ARGB_8888, true)
        ManipulationStrokeRenderer.render(
            Canvas(committed),
            committed,
            points,
            DrawingTool.SMUDGE,
            baseWidth = 20f,
            strength = 0.7f,
        )

        val session = ManipulationStrokeSession(source, DrawingTool.SMUDGE, baseWidth = 20f, strength = 0.7f)
        points.indices.forEach { index -> session.append(points.subList(0, index + 1)) }
        session.finish(points)

        assertArrayEquals(pixels(committed), pixels(session.copyResult()))
        val preview = requireNotNull(session.preview())
        assertTrue("short gesture must not allocate a document-sized preview", preview.bitmap.width < SIZE)
        assertTrue("short gesture must not allocate a document-sized preview", preview.bitmap.height < SIZE)
        session.release()
        session.releasePreview()
    }

    @Test
    fun manipulation_never_changes_an_unpassed_layer() {
        val activeLayer = sourceBitmap()
        val otherLayer = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val before = pixels(otherLayer)

        ManipulationStrokeRenderer.render(
            Canvas(activeLayer),
            activeLayer,
            stroke(),
            DrawingTool.BLUR,
            baseWidth = 20f,
            strength = 0.35f,
        )

        assertArrayEquals(before, pixels(otherLayer))
    }

    @Test
    fun blur_strength_zero_is_a_pixel_no_op() {
        val bitmap = sourceBitmap()
        val before = pixels(bitmap)
        ManipulationStrokeRenderer.render(
            Canvas(bitmap),
            bitmap,
            stroke(),
            DrawingTool.BLUR,
            baseWidth = 20f,
            strength = 0f,
        )
        assertArrayEquals(before, pixels(bitmap))
    }

    @Test
    fun smudge_moves_visible_pigment_in_drag_direction() {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawCircle(24f, 48f, 8f, android.graphics.Paint().apply { color = Color.RED })
        ManipulationStrokeRenderer.render(
            Canvas(bitmap),
            bitmap,
            listOf(TouchPoint(24f, 48f), TouchPoint(64f, 48f)),
            DrawingTool.SMUDGE,
            baseWidth = 20f,
            strength = 0.8f,
        )
        assertTrue(Color.red(bitmap.getPixel(60, 48)) > 0)
        assertEquals(0, Color.alpha(bitmap.getPixel(100, 48)))
    }

    private fun sourceBitmap(): Bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also { bitmap ->
        val paint = android.graphics.Paint().apply { color = Color.RED }
        Canvas(bitmap).drawCircle(24f, 48f, 9f, paint)
        paint.color = Color.BLUE
        Canvas(bitmap).drawCircle(46f, 48f, 9f, paint)
    }

    private fun stroke() = listOf(
        TouchPoint(24f, 48f, pressure = 1f, timestamp = 0L),
        TouchPoint(42f, 48f, pressure = 1f, velocity = 300f, timestamp = 16L),
        TouchPoint(64f, 48f, pressure = 1f, velocity = 600f, timestamp = 32L),
        TouchPoint(78f, 48f, pressure = 1f, velocity = 400f, timestamp = 48L),
    )

    private fun pixels(bitmap: Bitmap): IntArray = IntArray(SIZE * SIZE).also {
        bitmap.getPixels(it, 0, SIZE, 0, 0, SIZE, SIZE)
    }

    private companion object {
        const val SIZE = 128
    }
}
