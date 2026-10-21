// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.Serializable

@Serializable(with = TransformSerializerImpl::class)
data class Transform(val translation: Point2, val rotationDegrees: Float = 0f, val stretch: Point2 = Point2(1f, 1f)) {
    constructor(x: Float = 0f, y: Float = 0f, rotationDegrees: Float = 0f) :
        this(Point2(x, y), rotationDegrees)

    val x: Float get() = translation.x
    val y: Float get() = translation.y
    val scaleX: Float get() = stretch.x
    val scaleY: Float get() = stretch.y

    fun withPosition(x: Float = this.x, y: Float = this.y): Transform = copy(translation = Point2(x, y))
}
