// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.ActorArtworkKey")
data class ActorArtworkKey(val timeMicros: Long, val selections: Map<String, String>)
