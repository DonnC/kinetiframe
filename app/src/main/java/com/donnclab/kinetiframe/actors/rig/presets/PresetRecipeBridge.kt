package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetChannelValidationException
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeValidation
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeValidationException

internal fun validatePresetRecipe(recipe: ActorMotionPreset) {
    val neutral = recipe.document().recipe
    try {
        PresetRecipeValidation.requireValid(neutral)
    } catch (failure: PresetRecipeValidationException) {
        throw IllegalArgumentException(PresetValidationDiagnostics.recipe(failure.issue, recipe.schemaVersion))
    } catch (failure: PresetChannelValidationException) {
        throw IllegalArgumentException(PresetValidationDiagnostics.channel(failure.issue))
    }
}
