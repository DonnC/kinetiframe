package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.motion.MotionContacts
import com.donnclab.kinetiframe.engine.rigging.motion.MotionPose

object ActorMotionPose {
    fun followBendSide(graph: SceneGraph, contacts: Map<String, JointContact>): Map<String, JointContact> =
        withLegacyRigDiagnostics { MotionContacts.followBendSide(graph, contacts) }

    fun apply(
        rest: SceneGraph,
        rotations: Map<String, Float>,
        roots: Map<String, RootTranslation>,
        contacts: Map<String, JointContact>,
    ): SceneGraph = withLegacyRigDiagnostics { MotionPose.apply(rest, rotations, roots, contacts) }

    fun contactBone(graph: SceneGraph, selected: String?): String? = MotionContacts.contactBone(graph, selected)

    fun captureContact(graph: SceneGraph, id: String, selected: String?): JointContact =
        withLegacyRigDiagnostics { MotionContacts.captureContact(graph, id, selected) }
}
