package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinAffine
import com.donnclab.kinetiframe.engine.rigging.model.TextureBinding

/** Change artwork registration without touching joints, weights, source pixels or other variants. */
object ArtworkBindingTransform {
    internal fun sourceLocal(binding: TextureBinding): SkinAffine = try {
        ArtworkRegistration.sourceLocal(binding)
    } catch (
        failure: ArtworkRegistrationException,
    ) {
        throw IllegalArgumentException("Invalid artwork alignment", failure)
    }
    fun apply(graph: SceneGraph, part: String, previous: TextureBinding, updated: TextureBinding): TextureBinding {
        try {
            ArtworkRegistration.requireValid(updated)
        } catch (
            failure: ArtworkRegistrationException,
        ) {
            throw IllegalArgumentException("Failed requirement.", failure)
        }
        if (previous.skin == null) return ArtworkRegistration.realign(SkinAffine.Identity, previous, updated)
        sourceLocal(previous)
        return ArtworkRegistration.realign(ArtworkSkinning.worldTransforms(graph).getValue(part), previous, updated)
    }
}
