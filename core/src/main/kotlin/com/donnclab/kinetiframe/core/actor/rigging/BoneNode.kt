// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import kotlinx.serialization.Serializable

@Serializable(with = BoneNodeSerializerImpl::class)
data class BoneNode(
    val hierarchy: BoneHierarchy,
    val rest: BoneRest,
    val artwork: BoneArtwork = BoneArtwork(),
    val display: BoneDisplay = BoneDisplay(),
) {
    constructor(id: String, parentId: String? = null, name: String, length: Float) :
        this(BoneHierarchy(id, name, parentId), BoneRest(length))

    val id: String get() = hierarchy.id
    val parentId: String? get() = hierarchy.parentId
    val name: String get() = hierarchy.name
    val length: Float get() = rest.length
    val localTransform: Transform get() = rest.localTransform
    val ikChainRootId: String? get() = rest.ikChainRootId
    val textureSlotId: String? get() = artwork.textureSlotId
    val textureBinding: TextureBinding? get() = artwork.textureBinding
    val artworkVariants: Map<String, ArtworkVariant> get() = artwork.artworkVariants
    val visible: Boolean get() = display.visible
    val drawOrder: Int get() = display.drawOrder

    fun withHierarchy(id: String = this.id, parentId: String? = this.parentId, name: String = this.name): BoneNode =
        copy(hierarchy = BoneHierarchy(id, name, parentId))

    fun withRest(
        length: Float = this.length,
        localTransform: Transform = this.localTransform,
        ikChainRootId: String? = this.ikChainRootId,
    ): BoneNode = copy(rest = BoneRest(length, localTransform, ikChainRootId))

    fun withArtwork(
        textureSlotId: String? = this.textureSlotId,
        textureBinding: TextureBinding? = this.textureBinding,
        artworkVariants: Map<String, ArtworkVariant> = this.artworkVariants,
    ): BoneNode = copy(artwork = BoneArtwork(textureSlotId, textureBinding, artworkVariants))

    fun withDisplay(visible: Boolean = this.visible, drawOrder: Int = this.drawOrder): BoneNode =
        copy(display = BoneDisplay(visible, drawOrder))
}
