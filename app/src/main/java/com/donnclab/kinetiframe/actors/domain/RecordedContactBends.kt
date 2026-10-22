package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.RotationCurve
import com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseKey
import com.donnclab.kinetiframe.actors.rig.model.ActorPoseTrack
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph

/** Planted endpoints must not erase a deliberately recorded opposite knee/elbow bend.
 * Store only discrete direction changes in the existing contact track; keep anchor positions,
 * untouched limbs and post-take contact directions. No new document format is needed. */
object RecordedContactBends {
    fun merge(
        keys: List<ActorPoseKey>,
        rest: SceneGraph,
        curves: List<RotationCurve>,
        touched: Set<String>,
        times: Collection<Long>,
        start: Long,
        end: Long,
        duration: Long,
    ): List<ActorPoseKey> {
        if (keys.none { it.contacts.isNotEmpty() }) return keys
        val endpoints = rest.bones.values.filter { it.ikChainRootId in touched || it.id in touched }
            .filter { it.ikChainRootId != null }.map { it.id }.toSet()
        if (endpoints.isEmpty()) return keys
        val candidates = (
            keys.map { it.timeMicros } + times + start + end +
                listOfNotNull((end + 1).takeIf { it <= duration })
            ).distinct().sorted()
        var result = keys
        for (time in candidates) {
            val anchors = ActorPoseTrack.contacts(keys, time)
            val rotations =
                ActorPoseTrack.evaluate(keys, time, rest) + PerformanceEvaluator.curveRotations(curves, time)
            val graph = ActorMotionPose.apply(rest, rotations, emptyMap(), emptyMap())
            val contacts = if (time in start..end) {
                val directed = ActorMotionPose.followBendSide(graph, anchors)
                anchors.mapValues { (id, anchor) -> if (id in endpoints) directed.getValue(id) else anchor }
            } else {
                anchors
            }
            val original = keys.firstOrNull { it.timeMicros == time }
            if (original != null) {
                result = ActorPoseTrack.put(result, original.copy(contacts = contacts))
            } else if (contacts != ActorPoseTrack.contacts(result, time)) {
                result = ActorPoseTrack.put(
                    result,
                    ActorPoseKey(
                        time,
                        ActorPoseTrack.evaluate(keys, time, rest),
                        ActorPoseTrack.rootTranslations(keys, time, rest),
                        contacts,
                    ),
                )
            }
        }
        return result
    }
}
