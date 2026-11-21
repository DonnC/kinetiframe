package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.RigTraversalException
import com.donnclab.kinetiframe.engine.rigging.math.RigWorldTransforms
import com.donnclab.kinetiframe.engine.rigging.math.SkinDeformation
import com.donnclab.kinetiframe.engine.rigging.math.SkinDeformationException

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
