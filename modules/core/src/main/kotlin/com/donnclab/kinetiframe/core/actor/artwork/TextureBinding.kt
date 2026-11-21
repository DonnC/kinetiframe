// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2
import kotlinx.serialization.Serializable

@Serializable(with = TextureBindingSerializerImpl::class)
data class TextureBinding(
    val assetId: String,
    val pivot: Point2,
    val alignment: ArtworkAlignment = ArtworkAlignment(),
    val skin: ArtworkSkin? = null,
) {
    constructor(assetId: String, pivotX: Float, pivotY: Float) : this(assetId, Point2(pivotX, pivotY))

    val pivotX: Float get() = pivot.x
    val pivotY: Float get() = pivot.y
    val offsetX: Float get() = alignment.offset.x
    val offsetY: Float get() = alignment.offset.y
    val rotationDegrees: Float get() = alignment.rotationDegrees
    val scale: Float get() = alignment.scale
}

data class ArtworkAlignment(val offset: Point2 = Point2(0f, 0f), val rotationDegrees: Float = 0f, val scale: Float = 1f)
