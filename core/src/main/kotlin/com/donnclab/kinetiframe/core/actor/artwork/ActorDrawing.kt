// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.Serializable

@Serializable(with = ActorDrawingSerializerImpl::class)
data class ActorDrawing(
    val source: DrawingSource,
    val placement: DrawingPlacement,
    val binding: DrawingBinding,
    val composition: DrawingComposition = DrawingComposition(),
) {
    val inkAssetId: String get() = source.inkAssetId
    val colourAssetId: String get() = source.colourAssetId
    val width: Int get() = source.size.width
    val height: Int get() = source.size.height
    val originX: Float get() = placement.origin.x
    val originY: Float get() = placement.origin.y
    val actorWidth: Float get() = placement.extent.width
    val actorHeight: Float get() = placement.extent.height
    val skin: ArtworkSkin get() = binding.skin
    val regions: List<ActorDrawingRegion> get() = binding.regions
    val enabled: Boolean get() = composition.enabled
    val overlayParts: Set<String> get() = composition.overlayParts
    val underlayParts: Set<String> get() = composition.underlayParts

    fun withSkin(skin: ArtworkSkin): ActorDrawing = copy(binding = binding.copy(skin = skin))
    fun withEnabled(enabled: Boolean): ActorDrawing = copy(composition = composition.copy(enabled = enabled))

    companion object {
        const val INK_ADDRESS = "@drawing-ink"
        const val COLOUR_ADDRESS = "@drawing-colour"
    }
}
