// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class RigAffineArithmetic { RENDER_FLOAT_BASIS, SKIN_DOUBLE_PRODUCT }

object RigTransformMath {
    fun local(transform: Transform): SkinAffine =
        local(transform.translation, transform.rotationDegrees, transform.stretch)

    fun renderLocal(transform: Transform): SkinAffine = local(
        transform.translation,
        transform.rotationDegrees,
        transform.stretch,
        RigAffineArithmetic.RENDER_FLOAT_BASIS,
    )

    internal fun local(
        translation: Point2,
        rotationDegrees: Float,
        stretch: Point2,
        arithmetic: RigAffineArithmetic = RigAffineArithmetic.SKIN_DOUBLE_PRODUCT,
    ): SkinAffine {
        // Legacy rendering rounds the trigonometric basis before scale; skin binding rounds after it.
        return when (arithmetic) {
            RigAffineArithmetic.RENDER_FLOAT_BASIS -> {
                val angle = rotationDegrees * (PI / 180.0)
                val c = cos(angle).toFloat()
                val s = sin(angle).toFloat()
                SkinAffine(Point2(c * stretch.x, -s * stretch.y), Point2(s * stretch.x, c * stretch.y), translation)
            }

            RigAffineArithmetic.SKIN_DOUBLE_PRODUCT -> {
                val angle = rotationDegrees * PI / 180.0
                SkinAffine(
                    Point2((cos(angle) * stretch.x).toFloat(), (-sin(angle) * stretch.y).toFloat()),
                    Point2((sin(angle) * stretch.x).toFloat(), (cos(angle) * stretch.y).toFloat()),
                    translation,
                )
            }
        }
    }
}
