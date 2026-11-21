// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.ActorPoseKey")
data class ActorPoseKey(
    val timeMicros: Long,
    val rotations: Map<String, Float>,
    val rootTranslations: Map<String, RootTranslation> = emptyMap(),
    val contacts: Map<String, JointContact> = emptyMap(),
)
