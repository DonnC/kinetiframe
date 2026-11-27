package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/**
 * The handles a beginner drags while animating: the tip of every chain, whatever the actor is.
 * On a person these are the hands, feet and head; on a cow, its hooves, tail and head; on a flag,
 * its corners. Dragging a tip bends the chain behind it through IK, so nobody has to turn three
 * joints to place a hand. Every joint stays editable in Setup.
 */
object PoseHandles {
    fun ids(graph: SceneGraph): Set<String> {
        val children = graph.bones.values.groupBy { it.parentId }
        return graph.bones.values.filter { bone ->
            // A tip has no children, or only attachment pins such as eyes and a mouth.
            !ActorJointHandle.isAttachment(graph, bone.id) &&
                children[bone.id].orEmpty().all { ActorJointHandle.isAttachment(graph, it.id) }
        }.map { it.id }.toSet()
    }

    /** Whether a bone is drawn by something: a cutout, a skin, or a drawn figure's piece. */
    fun isCovered(graph: SceneGraph, id: String): Boolean {
        val bone = graph.bones[id] ?: return false
        if (bone.textureBinding != null) return true
        val figure = graph.figure ?: return false
        return figure.parts.any { it.boneId == id } ||
            figure.tubes.any { id in it.boneIds } ||
            figure.surfaces.any { surface -> surface.bones.any { it.boneId == id } }
    }
}
