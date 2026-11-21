// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.presets.AppliedActorPreset")
data class AppliedPresetDocument(val recipe: PresetRecipeDocument, val intensity: Float = 1f, val speed: Float = 1f)
