// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object MotionComposition {
    fun rotations(
        rest: SceneGraph,
        keyframes: Map<String, Float>,
        presetOffsets: Map<String, Float>,
        recorded: Map<String, Float>,
    ): Map<String, Float> {
        val preset = if (presetOffsets.isEmpty()) {
            emptyMap()
        } else {
            presetOffsets.mapValues { (id, offset) ->
                (rest.bones[id]?.localTransform?.rotationDegrees ?: 0f) + offset
            }
        }
        return overlay(keyframes, preset, recorded)
    }

    fun rootTranslations(
        keyframes: Map<String, RootTranslation>,
        preset: Map<String, RootTranslation>,
        recorded: Map<String, RootTranslation>,
    ): Map<String, RootTranslation> = overlay(keyframes, preset, recorded)

    // Authored channels override whole values, including zero; they are not additive blend weights.
    private fun <T> overlay(
        keyframes: Map<String, T>,
        preset: Map<String, T>,
        recorded: Map<String, T>,
    ): Map<String, T> = keyframes + preset + recorded
}
