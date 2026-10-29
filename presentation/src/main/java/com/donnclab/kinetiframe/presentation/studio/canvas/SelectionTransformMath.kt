package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.core.geometry.Affine2D
import kotlin.math.abs
import kotlin.math.atan2

/** Absolute gesture math: no accumulated drift, hit-radius jump, or TRS-only decomposition. */
object SelectionTransformMath {
    data class TransformInfo(val translation: Offset, val scale: Offset, val rotation: Float)
    fun getTransformInfo(matrix: Matrix): TransformInfo {
        val v = matrix.values
        return TransformInfo(
            Offset(v[12], v[13]),
            Offset(kotlin.math.hypot(v[0], v[1]), kotlin.math.hypot(v[4], v[5])),
            Math.toDegrees(atan2(v[1].toDouble(), v[0].toDouble())).toFloat(),
        )
    }

    fun compute(
        startMatrix: Matrix,
        bounds: Rect,
        handle: Int,
        startTouchDoc: Offset,
        currentTouchDoc: Offset,
        gestureScale: Float = 1f,
        customPivotDoc: Offset? = null,
        lockAspectRatio: Boolean = true,
        skewMode: Boolean = false,
    ): Matrix {
        if (handle == 4 || startTouchDoc == currentTouchDoc) return Matrix(startMatrix.values.copyOf())
        val start = startMatrix.affine()
        if (handle !in 0..9) {
            val delta = currentTouchDoc - startTouchDoc
            return (Affine2D(tx = delta.x, ty = delta.y) * start).matrix()
        }
        if (handle == 5) {
            // Rotate in stage space, not in an already stretched/sheared local coordinate system.
            val pivot = customPivotDoc ?: startMatrix.map(bounds.center)
            val from = startTouchDoc - pivot
            val to = currentTouchDoc - pivot
            if (from.getDistance() < .001f || to.getDistance() < .001f) return Matrix(startMatrix.values.copyOf())
            val angle = atan2(to.y, to.x) - atan2(from.y, from.x)
            val c = kotlin.math.cos(angle)
            val s = kotlin.math.sin(angle)
            return (
                Affine2D(tx = pivot.x, ty = pivot.y) * Affine2D(c, s, -s, c) *
                    Affine2D(tx = -pivot.x, ty = -pivot.y) * start
                ).matrix()
        }
        if (abs(start.a * start.d - start.b * start.c) < .000001f) return Matrix(startMatrix.values.copyOf())
        val inverse = Matrix(startMatrix.values.copyOf()).apply { invert() }
        val from = inverse.map(startTouchDoc)
        val to = inverse.map(currentTouchDoc)
        val pivot = when (handle) {
            0 -> bounds.bottomRight
            1 -> bounds.bottomLeft
            2 -> bounds.topLeft
            3 -> bounds.topRight
            6 -> Offset(bounds.center.x, bounds.bottom)
            7 -> Offset(bounds.left, bounds.center.y)
            8 -> Offset(bounds.center.x, bounds.top)
            else -> Offset(bounds.right, bounds.center.y)
        }
        val delta = to - from
        val dx = safe(from.x - pivot.x)
        val dy = safe(from.y - pivot.y)
        val local = if (skewMode && handle in 6..9) {
            if (handle == 6 || handle == 8) {
                Affine2D(c = delta.x / dy)
            } else {
                Affine2D(b = delta.y / dx)
            }
        } else {
            var sx = if (handle == 6 || handle == 8) 1f else 1f + delta.x / dx
            var sy = if (handle == 7 || handle == 9) 1f else 1f + delta.y / dy
            if (handle in 0..3 && lockAspectRatio) {
                // Projection is continuous even when the dragged corner passes a diagonal.
                val vector = from - pivot
                val denominator = vector.x * vector.x + vector.y * vector.y
                val scale = if (denominator >
                    .0001f
                ) {
                    1f + (delta.x * vector.x + delta.y * vector.y) / denominator
                } else {
                    1f
                }
                sx = scale
                sy = scale
            }
            Affine2D(a = nonZero(sx), d = nonZero(sy))
        }
        return (start * Affine2D(tx = pivot.x, ty = pivot.y) * local * Affine2D(tx = -pivot.x, ty = -pivot.y)).matrix()
    }
    private fun safe(value: Float) = if (abs(value) < .001f) if (value < 0f) -.001f else .001f else value
    private fun nonZero(value: Float) = if (abs(value) < .01f) if (value < 0f) -.01f else .01f else value
    private fun Matrix.affine() = values.let { Affine2D(it[0], it[1], it[4], it[5], it[12], it[13]) }
    private fun Affine2D.matrix() = Matrix().also { matrix ->
        matrix.values[0] = a
        matrix.values[1] = b
        matrix.values[4] = c
        matrix.values[5] = d
        matrix.values[12] = tx
        matrix.values[13] = ty
    }
}
