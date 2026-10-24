package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetValidationDiagnostics

internal fun validatePresetRecipe(recipe: ActorMotionPreset) {
    PresetValidationDiagnostics.requireValidRecipe(recipe.document().recipe)
}
