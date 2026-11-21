// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding

data class BoneArtwork(
    val textureSlotId: String? = null,
    val textureBinding: TextureBinding? = null,
    val artworkVariants: Map<String, ArtworkVariant> = emptyMap(),
)
