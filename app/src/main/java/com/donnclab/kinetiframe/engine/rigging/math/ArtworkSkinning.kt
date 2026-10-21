package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.model.ArtworkSkin
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinAffine

object ArtworkSkinning {
    fun worldTransforms(graph: SceneGraph): Map<String, SkinAffine> = try {
        RigWorldTransforms.all(graph)
    } catch (failure: RigTraversalException) {
        throw IllegalArgumentException("Invalid skin hierarchy")
    }

    fun deform(skin: ArtworkSkin, world: Map<String, SkinAffine>): FloatArray = try {
        SkinDeformation.deform(skin, world)
    } catch (failure: SkinDeformationException) {
        throw IllegalArgumentException("Missing skin bone", failure)
    }
}
