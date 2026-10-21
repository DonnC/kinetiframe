// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.kinetiframe.engine.rigging.model.ActorView")
enum class PresetAuthoredView {
    ORIGINAL,
    FRONT,
    PROFILE,
    THREE_QUARTER,
    BACK,
    ;

    fun supportsView(view: PresetAuthoredView): Boolean = view == ORIGINAL || view == this
}

@Serializable(with = PresetRecipeDocumentSerializerImpl::class)
data class PresetRecipeDocument(
    val recipe: PresetRecipe,
    val description: String,
    val authoredView: PresetAuthoredView = PresetAuthoredView.FRONT,
) {
    fun supportsView(view: PresetAuthoredView): Boolean = authoredView.supportsView(view)
}
