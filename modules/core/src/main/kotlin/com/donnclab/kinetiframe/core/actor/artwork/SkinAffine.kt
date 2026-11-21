// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.Serializable
import kotlin.math.abs

enum class SkinTransformIssue { SINGULAR_BIND }
class SkinTransformException(val issue: SkinTransformIssue) : IllegalArgumentException(issue.name)

@Serializable(with = SkinAffineSerializer::class)
data class SkinAffine(val xRow: Point2, val yRow: Point2, val origin: Point2) {
    val a get() = xRow.x
    val b get() = xRow.y
    val c get() = yRow.x
    val d get() = yRow.y
    val tx get() = origin.x
    val ty get() = origin.y

    fun x(x: Float, y: Float) = a * x + b * y + tx
    fun y(x: Float, y: Float) = c * x + d * y + ty
    fun times(other: SkinAffine) = SkinAffine(
        Point2(a * other.a + b * other.c, a * other.b + b * other.d),
        Point2(c * other.a + d * other.c, c * other.b + d * other.d),
        Point2(x(other.tx, other.ty), y(other.tx, other.ty)),
    )

    fun inverse(): SkinAffine {
        val determinant = a * d - b * c
        if (!determinant.isFinite() || abs(determinant) <= MINIMUM_DETERMINANT) {
            throw SkinTransformException(SkinTransformIssue.SINGULAR_BIND)
        }
        return SkinAffine(
            Point2(d / determinant, -b / determinant),
            Point2(-c / determinant, a / determinant),
            Point2((b * ty - d * tx) / determinant, (c * tx - a * ty) / determinant),
        )
    }

    companion object {
        private const val MINIMUM_DETERMINANT = 1e-8f
        val Identity = SkinAffine(Point2(1f, 0f), Point2(0f, 1f), Point2(0f, 0f))
    }
}
