// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import com.donnclab.kinetiframe.core.geometry.Affine2D
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.core.geometry.Rect2

/** Rig and studio logic work in platform-neutral geometry; screens convert at the boundary. */
fun Offset.toPoint2(): Point2 = Point2(x, y)

fun Point2.toOffset(): Offset = Offset(x, y)

fun Rect.toRect2(): Rect2 = Rect2(left, top, right, bottom)

fun Rect2.toRect(): Rect = Rect(left, top, right, bottom)

/** The 2D part of a Compose [Matrix]; selection transforms never use depth or perspective. */
fun Matrix.toAffine2D(): Affine2D = values.let { Affine2D(it[0], it[1], it[4], it[5], it[12], it[13]) }

fun Affine2D.toMatrix(): Matrix = Matrix().also { matrix ->
    matrix.values[0] = a
    matrix.values[1] = b
    matrix.values[4] = c
    matrix.values[5] = d
    matrix.values[12] = tx
    matrix.values[13] = ty
}
