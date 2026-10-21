// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkAlignment
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtworkPlacementGestureTest {
    private val original = TextureBinding("owned", 10f, 12f)
    private val basis = ArtworkGestureBasis(1f, 0f)

    @Test fun `touch placement respects viewport zoom and rotated bone coordinates`() {
        val binding = original.copy(alignment = ArtworkAlignment(scale = 2f))
        val moved = ArtworkPlacementGesture.apply(
            binding,
            ArtworkGestureDelta(Point2(0f, 20f), 1.5f, 30f),
            ArtworkGestureBasis(2f, 90f),
        )
        assertEquals(10f, moved.offsetX, .001f)
        assertEquals(0f, moved.offsetY, .001f)
        assertEquals(3f, moved.scale)
        assertEquals(30f, moved.rotationDegrees)
        assertEquals(binding.assetId, moved.assetId)
        assertEquals(binding.pivot, moved.pivot)
    }

    @Test fun `invalid gestures preserve binding and scale stays bounded`() {
        assertEquals(
            original,
            ArtworkPlacementGesture.apply(original, ArtworkGestureDelta(Point2(Float.NaN, 0f)), basis),
        )
        assertEquals(
            original,
            ArtworkPlacementGesture.apply(
                original,
                ArtworkGestureDelta(Point2(0f, 0f)),
                basis.copy(viewportScale = 0f),
            ),
        )
        assertEquals(original, ArtworkPlacementGesture.apply(original, ArtworkGestureDelta(Point2(0f, 0f), -1f), basis))
        assertEquals(
            ArtworkRegistration.MAX_SCALE,
            ArtworkPlacementGesture.apply(original, ArtworkGestureDelta(Point2(0f, 0f), 1000f), basis).scale,
        )
        assertEquals(
            ArtworkRegistration.MIN_SCALE,
            ArtworkPlacementGesture.apply(original, ArtworkGestureDelta(Point2(0f, 0f), .00001f), basis).scale,
        )
    }

    @Test fun `finite inputs overflowing offset or rotation cannot poison binding`() {
        val binding = original.copy(alignment = ArtworkAlignment(Point2(Float.MAX_VALUE, 0f), Float.MAX_VALUE))
        assertEquals(
            binding,
            ArtworkPlacementGesture.apply(binding, ArtworkGestureDelta(Point2(Float.MAX_VALUE, 0f)), basis),
        )
        assertEquals(
            binding,
            ArtworkPlacementGesture.apply(
                binding,
                ArtworkGestureDelta(Point2(0f, 0f), rotationDegrees = Float.MAX_VALUE),
                basis,
            ),
        )
        assertEquals(
            original,
            ArtworkPlacementGesture.apply(
                original,
                ArtworkGestureDelta(Point2(1f, 0f)),
                basis.copy(viewportScale = Float.MIN_VALUE),
            ),
        )
    }
}
