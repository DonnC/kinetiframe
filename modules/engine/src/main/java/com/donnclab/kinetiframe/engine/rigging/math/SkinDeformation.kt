// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class SkinDeformationIssue { MISSING_WORLD_TRANSFORM }
class SkinDeformationException(val issue: SkinDeformationIssue, val boneId: String) :
    IllegalArgumentException(issue.name)

// Quaternion blending preserves sleeve volume; non-rigid deltas retain affine fallback.
object SkinDeformation {
    private const val RIGID_TOLERANCE = .0001f
    fun deform(skin: ArtworkSkin, world: Map<String, SkinAffine>): FloatArray {
        val deltas = skin.bones.map { influence ->
            val posed = world[influence.boneId] ?: throw SkinDeformationException(
                SkinDeformationIssue.MISSING_WORLD_TRANSFORM,
                influence.boneId,
            )
            posed.times(influence.bindWorld.inverse())
        }
        val rigid = deltas.all {
            abs(it.a * it.a + it.c * it.c - 1f) < RIGID_TOLERANCE &&
                abs(it.b * it.b + it.d * it.d - 1f) < RIGID_TOLERANCE &&
                abs(it.a * it.b + it.c * it.d) < RIGID_TOLERANCE &&
                it.a * it.d - it.b * it.c > 0f
        }
        // Quaternion components and dual translations are prepared once per bone, not vertex.
        val rotations = deltas.map { atan2(it.c.toDouble(), it.a.toDouble()) / 2.0 }
        val qw = rotations.map(::cos)
        val qz = rotations.map(::sin)
        val dx = deltas.indices.map { .5 * (deltas[it].tx * qw[it] + deltas[it].ty * qz[it]) }
        val dy = deltas.indices.map { .5 * (deltas[it].ty * qw[it] - deltas[it].tx * qz[it]) }
        return FloatArray(skin.vertices.size * 2).also { output ->
            skin.vertices.forEachIndexed { index, vertex ->
                if (!rigid) {
                    var x = 0f
                    var y = 0f
                    deltas.forEachIndexed { bone, delta ->
                        x += vertex.weights[bone] * delta.x(vertex.x, vertex.y)
                        y += vertex.weights[bone] * delta.y(vertex.x, vertex.y)
                    }
                    output[index * 2] = x
                    output[index * 2 + 1] = y
                } else {
                    val reference = vertex.weights.indices.maxBy { vertex.weights[it] }
                    var w = 0.0
                    var z = 0.0
                    var dualX = 0.0
                    var dualY = 0.0
                    vertex.weights.forEachIndexed { bone, weight ->
                        // q and -q describe the same rotation. Align hemispheres before blending
                        // so a 179° / -179° transition follows the short arc, never spins through 0°.
                        val signed = weight * if (qw[reference] * qw[bone] + qz[reference] * qz[bone] < 0) -1 else 1
                        w += signed * qw[bone]
                        z += signed * qz[bone]
                        dualX += signed * dx[bone]
                        dualY += signed * dy[bone]
                    }
                    val norm = hypot(w, z)
                    w /= norm
                    z /= norm
                    dualX /= norm
                    dualY /= norm
                    output[index * 2] =
                        ((w * w - z * z) * vertex.x - 2 * w * z * vertex.y + 2 * (dualX * w - dualY * z)).toFloat()
                    output[index * 2 + 1] =
                        (2 * w * z * vertex.x + (w * w - z * z) * vertex.y + 2 * (dualX * z + dualY * w)).toFloat()
                }
            }
        }
    }
}
