// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.DrawingPoint")
data class DrawingPoint(val x: Float, val y: Float)

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.ActorDrawingRegion")
data class ActorDrawingRegion(
    val boneId: String,
    val outline: List<DrawingPoint>,
    val featherPixels: Float = DEFAULT_FEATHER,
) {
    companion object {
        private const val DEFAULT_FEATHER = 12f
    }
}
