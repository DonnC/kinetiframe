package com.donnclab.kinetiframe.engine.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * The single affine boundary between a device viewport and a project stage.
 *
 * Matrix values use Android's 3×3 affine layout. Keeping this math independent from
 * `android.graphics.Matrix` makes the contract testable on the local JVM and usable by
 * drawing, selection, guides, rig overlays and headless rendering.
 */
class DocumentCoordinateTransform(private val stage: StageDimensions, matrixValues: FloatArray) {
    private val values = matrixValues.copyOf().also {
        require(it.size == MATRIX_VALUE_COUNT) { "Expected a 3×3 matrix." }
    }

    fun toViewport(documentPoint: Offset): Offset = Offset(
        x = values[SCALE_X] * documentPoint.x + values[SKEW_X] * documentPoint.y + values[TRANSLATE_X],
        y = values[SKEW_Y] * documentPoint.x + values[SCALE_Y] * documentPoint.y + values[TRANSLATE_Y],
    )

    /** Returns null when the render transform is singular and cannot be safely inverted. */
    fun toDocument(viewportPoint: Offset): Offset? {
        val determinant = values[SCALE_X] * values[SCALE_Y] - values[SKEW_X] * values[SKEW_Y]
        if (abs(determinant) < MIN_INVERTIBLE_DETERMINANT) return null

        val translatedX = viewportPoint.x - values[TRANSLATE_X]
        val translatedY = viewportPoint.y - values[TRANSLATE_Y]
        return Offset(
            x = (values[SCALE_Y] * translatedX - values[SKEW_X] * translatedY) / determinant,
            y = (-values[SKEW_Y] * translatedX + values[SCALE_X] * translatedY) / determinant,
        )
    }

    fun contains(documentPoint: Offset): Boolean = documentPoint.x in 0f..stage.width.toFloat() &&
        documentPoint.y in 0f..stage.height.toFloat()

    companion object {
        private const val MATRIX_VALUE_COUNT = 9
        private const val SCALE_X = 0
        private const val SKEW_X = 1
        private const val TRANSLATE_X = 2
        private const val SKEW_Y = 3
        private const val SCALE_Y = 4
        private const val TRANSLATE_Y = 5
        private const val MIN_INVERTIBLE_DETERMINANT = 0.000001f
    }
}
