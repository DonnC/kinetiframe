package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.PresetAuthoredView
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.core.actor.rigging.ActorView

internal fun ActorView.neutral(): PresetAuthoredView = when (this) {
    ActorView.ORIGINAL -> PresetAuthoredView.ORIGINAL
    ActorView.FRONT -> PresetAuthoredView.FRONT
    ActorView.PROFILE -> PresetAuthoredView.PROFILE
    ActorView.THREE_QUARTER -> PresetAuthoredView.THREE_QUARTER
    ActorView.BACK -> PresetAuthoredView.BACK
}

internal fun PresetAuthoredView.product(): ActorView = when (this) {
    PresetAuthoredView.ORIGINAL -> ActorView.ORIGINAL
    PresetAuthoredView.FRONT -> ActorView.FRONT
    PresetAuthoredView.PROFILE -> ActorView.PROFILE
    PresetAuthoredView.THREE_QUARTER -> ActorView.THREE_QUARTER
    PresetAuthoredView.BACK -> ActorView.BACK
}

internal fun ActorMotionPreset.document(): PresetRecipeDocument = PresetRecipeDocument(
    PresetRecipe(
        PresetRecipeMetadata(schemaVersion, id, name, durationSeconds),
        PresetRecipeChannels(requiredBones, PresetRotationValidationInput(channels), rootMotion),
        loop,
    ),
    description,
    authoredView.neutral(),
)

internal fun PresetRecipeDocument.product(): ActorMotionPreset {
    val metadata = recipe.metadata
    return ActorMotionPreset(
        metadata.schemaVersion, metadata.id, metadata.name, description,
        metadata.durationSeconds, recipe.channels.requiredBones, recipe.channels.rotations.map { it.product() },
        recipe.loop, rootMotion = recipe.channels.roots, authoredView = authoredView.product(),
    )
}
