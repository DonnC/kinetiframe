package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument

/**
 * A recipe in the preset catalog, with the icon shipped beside it. The icon is resolved from the
 * paired asset filename, never from a recipe ID, and is not part of the saved recipe.
 */
data class ActorPreset(val recipe: PresetRecipeDocument, val previewAssetPath: String? = null)
