// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Authored 2D drawings, not camera rotation or interchangeable joint angles. The names are saved;
 * screens name each view from their own string resources.
 */
@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.ActorView")
enum class ActorView {
    ORIGINAL,
    FRONT,
    PROFILE,
    THREE_QUARTER,
    BACK,
}
