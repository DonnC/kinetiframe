// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve

class CompiledPoseTrack(keys: List<ActorPoseKey>, rest: SceneGraph, loop: Boolean = false) {
    private data class RootChannels(val x: CompiledMotionCurve, val y: CompiledMotionCurve)
    private val rotations: Map<String, CompiledMotionCurve>
    private val roots: Map<String, RootChannels>

    init {
        val ordered = keys.sortedBy { it.timeMicros }
        val times = ordered.map { it.timeMicros }
        rotations = if (ordered.isEmpty()) {
            emptyMap()
        } else {
            rest.bones.mapValues { (id, bone) ->
                CompiledMotionCurve(
                    times,
                    ordered.map { it.rotations[id] ?: bone.localTransform.rotationDegrees },
                    angular = true,
                    loop = loop,
                )
            }
        }
        roots = if (ordered.isEmpty()) {
            emptyMap()
        } else {
            rest.rootBoneIds.associateWith { id ->
                RootChannels(
                    CompiledMotionCurve(times, ordered.map { it.rootTranslations[id]?.x ?: 0f }, loop = loop),
                    CompiledMotionCurve(times, ordered.map { it.rootTranslations[id]?.y ?: 0f }, loop = loop),
                )
            }
        }
    }

    fun rotations(
        timeMicros: Long,
        interpolation: CompiledMotionCurve.Interpolation = CompiledMotionCurve.Interpolation.SMOOTH,
    ): Map<String, Float> = rotations.mapValues { (_, curve) -> curve.sample(timeMicros, interpolation) }

    fun rootTranslations(
        timeMicros: Long,
        interpolation: CompiledMotionCurve.Interpolation = CompiledMotionCurve.Interpolation.SMOOTH,
    ): Map<String, RootTranslation> = roots.mapValues { (_, channels) ->
        RootTranslation(channels.x.sample(timeMicros, interpolation), channels.y.sample(timeMicros, interpolation))
    }
}
