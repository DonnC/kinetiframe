package com.donnclab.kinetiframe.studio.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCoordinateTransformTest {
    @Test
    fun `round trips a rotated and translated stage point`() {
        val transform = DocumentCoordinateTransform(
            stage = StageDimensions.LandscapeHd,
            matrixValues = floatArrayOf(0f, -2f, 1_000f, 2f, 0f, 200f, 0f, 0f, 1f),
        )
        val documentPoint = Offset(100f, 200f)

        val viewportPoint = transform.toViewport(documentPoint)
        val mappedBack = requireNotNull(transform.toDocument(viewportPoint))

        assertEquals(600f, viewportPoint.x, 0.001f)
        assertEquals(400f, viewportPoint.y, 0.001f)
        assertEquals(documentPoint.x, mappedBack.x, 0.001f)
        assertEquals(documentPoint.y, mappedBack.y, 0.001f)
    }

    @Test
    fun `stage containment rejects letterbox coordinates after mapping`() {
        val transform = DocumentCoordinateTransform(
            stage = StageDimensions.Square,
            matrixValues = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        )

        assertTrue(transform.contains(Offset(1_080f, 1_080f)))
        assertFalse(transform.contains(Offset(-1f, 540f)))
        assertFalse(transform.contains(Offset(540f, 1_081f)))
    }

    @Test
    fun `singular matrix rejects inverse mapping`() {
        val transform = DocumentCoordinateTransform(
            stage = StageDimensions.Square,
            matrixValues = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f),
        )

        assertNull(transform.toDocument(Offset.Zero))
    }
}
