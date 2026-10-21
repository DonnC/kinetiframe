// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object PoseKeys {
    fun put(keys: List<ActorPoseKey>, key: ActorPoseKey): List<ActorPoseKey> =
        (keys.filterNot { it.timeMicros == key.timeMicros } + key).sortedBy { it.timeMicros }

    // Contacts hold in authored list order; sorting here would change legacy malformed-track behavior.
    fun contacts(keys: List<ActorPoseKey>, timeMicros: Long): Map<String, JointContact> =
        keys.lastOrNull { it.timeMicros <= timeMicros }?.contacts.orEmpty()

    fun capture(
        time: Long,
        posed: SceneGraph,
        rest: SceneGraph,
        contacts: Map<String, JointContact> = emptyMap(),
    ): ActorPoseKey = ActorPoseKey(
        time,
        posed.bones.mapValues { it.value.localTransform.rotationDegrees },
        rest.rootBoneIds.associateWith { id ->
            val posedRoot = posed.bones.getValue(id).localTransform
            val restRoot = rest.bones.getValue(id).localTransform
            RootTranslation(posedRoot.x - restRoot.x, posedRoot.y - restRoot.y)
        },
        contacts,
    )
}
