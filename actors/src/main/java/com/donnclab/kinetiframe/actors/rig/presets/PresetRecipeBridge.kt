package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeValidation

/** Throws the typed validation failure for the first problem in [recipe]. */
internal fun validatePresetRecipe(recipe: ActorMotionPreset) {
    PresetRecipeValidation.requireValid(recipe.document().recipe)
}
