// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.geometry.Point2

object ArtworkRegistration {
    const val MIN_SCALE = .001f
    const val MAX_SCALE = 100f

    fun requireValid(binding: TextureBinding) {
        if (!binding.pivotX.isFinite() || !binding.pivotY.isFinite() ||
            !binding.offsetX.isFinite() || !binding.offsetY.isFinite() ||
            !binding.rotationDegrees.isFinite() || !binding.scale.isFinite() ||
            binding.scale !in MIN_SCALE..MAX_SCALE
        ) {
            throw ArtworkRegistrationException(ArtworkRegistrationIssue.INVALID_ALIGNMENT)
        }
    }

    fun sourceLocal(binding: TextureBinding): SkinAffine {
        requireValid(binding)
        return RigTransformMath.local(
            binding.alignment.offset,
            binding.rotationDegrees,
            Point2(binding.scale, binding.scale),
        )
            .times(SkinAffine.Identity.copy(origin = Point2(-binding.pivotX, -binding.pivotY)))
    }

    fun realign(world: SkinAffine, previous: TextureBinding, updated: TextureBinding): TextureBinding {
        requireValid(updated)
        val skin = previous.skin ?: return updated.copy(assetId = previous.assetId, skin = null)
        // Vertices are actor-space, so the registration delta must include the bone's world basis.
        val delta = world.times(sourceLocal(updated)).times(world.times(sourceLocal(previous)).inverse())
        return updated.copy(
            assetId = previous.assetId,
            skin = skin.copy(
                mesh = skin.mesh.copy(
                    vertices = skin.vertices.map {
                        it.copy(x = delta.x(it.x, it.y), y = delta.y(it.x, it.y))
                    },
                ),
            ),
        )
    }
}

enum class ArtworkRegistrationIssue { INVALID_ALIGNMENT }
class ArtworkRegistrationException(val issue: ArtworkRegistrationIssue) : IllegalArgumentException(issue.name)
