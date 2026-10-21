// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import kotlin.math.cos
import kotlin.math.sin

data class ArtworkGestureDelta(val pan: Point2, val zoom: Float = 1f, val rotationDegrees: Float = 0f)
data class ArtworkGestureBasis(val viewportScale: Float, val boneAngle: Float)

object ArtworkPlacementGesture {
    fun apply(binding: TextureBinding, delta: ArtworkGestureDelta, basis: ArtworkGestureBasis): TextureBinding {
        if (!delta.pan.x.isFinite() || !delta.pan.y.isFinite() || !basis.viewportScale.isFinite() ||
            !basis.boneAngle.isFinite() || !delta.zoom.isFinite() || !delta.rotationDegrees.isFinite() ||
            basis.viewportScale <= 0f || delta.zoom <= 0f
        ) {
            return binding
        }
        val angle = Math.toRadians(-basis.boneAngle.toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        val x = delta.pan.x / basis.viewportScale
        val y = delta.pan.y / basis.viewportScale
        val offset = Point2(binding.offsetX + (x * c - y * s), binding.offsetY + (x * s + y * c))
        val updated = binding.copy(
            alignment = binding.alignment.copy(
                offset = offset,
                scale = (binding.scale * delta.zoom).coerceIn(
                    ArtworkRegistration.MIN_SCALE,
                    ArtworkRegistration.MAX_SCALE,
                ),
                rotationDegrees = binding.rotationDegrees + delta.rotationDegrees,
            ),
        )
        return updated.takeIf { it.offsetX.isFinite() && it.offsetY.isFinite() && it.rotationDegrees.isFinite() }
            ?: binding
    }
}
