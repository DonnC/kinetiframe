package com.donnclab.kinetiframe.actors.rig.model

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.JointContact
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.PoseSampling

/** Rest-relative motion survives rig placement changes. Tiny identity cache holds four immutable
 * compiled tracks. Content fallback also reuses immutable documents decoded by disk render paths;
 * keys/rest revisions are never sorted/copied into spline arrays on a cache hit. */
internal object LegacyPoseTrackReference {
    private data class Track(
        val keys: List<ActorPoseKey>,
        val rest: SceneGraph,
        val loop: Boolean,
        val channels: Channels,
    ) {
        val rotations get() = channels.rotations
        val roots get() = channels.roots
    }
    private data class Channels(
        val rotations: Map<String, CompiledMotionCurve>,
        val roots: Map<String, Pair<CompiledMotionCurve, CompiledMotionCurve>>,
    )
    private val cache = ArrayDeque<Track>()
    private fun compiled(keys: List<ActorPoseKey>, rest: SceneGraph, loop: Boolean): Track = synchronized(cache) {
        cache.firstOrNull {
            it.loop == loop && (it.keys === keys || it.keys == keys) &&
                (it.rest === rest || it.rest == rest)
        }?.let { return@synchronized it }
        val ordered = keys.sortedBy { it.timeMicros }
        val times = ordered.map { it.timeMicros }
        val rotations = rest.bones.mapValues { (id, bone) ->
            CompiledMotionCurve(
                times,
                ordered.map { it.rotations[id] ?: bone.localTransform.rotationDegrees },
                angular = true,
                loop = loop,
            )
        }
        val roots = rest.rootBoneIds.associateWith { id ->
            CompiledMotionCurve(times, ordered.map { it.rootTranslations[id]?.x ?: 0f }, loop = loop) to
                CompiledMotionCurve(times, ordered.map { it.rootTranslations[id]?.y ?: 0f }, loop = loop)
        }
        Track(keys, rest, loop, Channels(rotations, roots)).also { track ->
            cache.addLast(track)
            if (cache.size > 4) cache.removeFirst()
        }
    }
    fun put(keys: List<ActorPoseKey>, key: ActorPoseKey): List<ActorPoseKey> =
        (keys.filterNot { it.timeMicros == key.timeMicros } + key).sortedBy { it.timeMicros }

    fun evaluate(
        keys: List<ActorPoseKey>,
        timeMicros: Long,
        rest: SceneGraph,
        sampling: PoseSampling = PoseSampling(),
    ): Map<String, Float> {
        if (keys.isEmpty()) return emptyMap()
        return compiled(keys, rest, sampling.loop).rotations.mapValues { (_, curve) ->
            curve.sample(timeMicros, sampling.interpolation)
        }
    }

    fun rootTranslations(
        keys: List<ActorPoseKey>,
        timeMicros: Long,
        rest: SceneGraph,
        sampling: PoseSampling = PoseSampling(),
    ): Map<String, RootTranslation> {
        if (keys.isEmpty()) return emptyMap()
        return compiled(keys, rest, sampling.loop).roots.mapValues { (_, curves) ->
            RootTranslation(
                curves.first.sample(timeMicros, sampling.interpolation),
                curves.second.sample(timeMicros, sampling.interpolation),
            )
        }
    }

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
            val a = posed.bones.getValue(id).localTransform
            val b = rest.bones.getValue(id).localTransform
            RootTranslation(a.x - b.x, a.y - b.y)
        },
        contacts,
    )
}
