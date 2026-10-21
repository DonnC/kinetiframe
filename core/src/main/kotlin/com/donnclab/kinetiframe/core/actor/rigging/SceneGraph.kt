// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.SceneGraph")
data class SceneGraph(
    val rootBoneIds: List<String> = emptyList(),
    val bones: Map<String, BoneNode> = emptyMap(),
    val drawing: ActorDrawing? = null,
)
