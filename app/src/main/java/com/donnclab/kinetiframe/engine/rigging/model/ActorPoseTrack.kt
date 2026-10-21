package com.donnclab.kinetiframe.engine.rigging.model

import com.donnclab.kinetiframe.engine.rigging.motion.PoseKeys
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling
import com.donnclab.kinetiframe.engine.rigging.motion.PoseTrackCache

typealias ActorPoseKey = com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
typealias JointContact = com.donnclab.kinetiframe.core.actor.motion.JointContact

object ActorPoseTrack {
    private val cache = PoseTrackCache()

    fun put(keys: List<ActorPoseKey>, key: ActorPoseKey): List<ActorPoseKey> = PoseKeys.put(keys, key)

    fun evaluate(
        keys: List<ActorPoseKey>,
        timeMicros: Long,
        rest: SceneGraph,
        sampling: PoseSampling = PoseSampling(),
    ): Map<String, Float> = if (keys.isEmpty()) {
        emptyMap()
    } else {
        cache.compiled(
            keys,
            rest,
            sampling.loop,
        ).rotations(timeMicros, sampling.interpolation)
    }

    fun rootTranslations(
        keys: List<ActorPoseKey>,
        timeMicros: Long,
        rest: SceneGraph,
        sampling: PoseSampling = PoseSampling(),
    ): Map<String, RootTranslation> = if (keys.isEmpty()) {
        emptyMap()
    } else {
        cache.compiled(
            keys,
            rest,
            sampling.loop,
        ).rootTranslations(timeMicros, sampling.interpolation)
    }

    fun contacts(keys: List<ActorPoseKey>, timeMicros: Long): Map<String, JointContact> =
        PoseKeys.contacts(keys, timeMicros)

    fun capture(
        time: Long,
        posed: SceneGraph,
        rest: SceneGraph,
        contacts: Map<String, JointContact> = emptyMap(),
    ): ActorPoseKey = PoseKeys.capture(time, posed, rest, contacts)
}
