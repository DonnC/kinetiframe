// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlin.math.abs

internal object PresetRootValidation {
    fun requireValid(channels: List<RootMotionChannel>, requiredBones: List<String>, loop: Boolean) {
        requireRecipe(
            channels.size <= MAX_CHANNELS && channels.map {
                it.boneId
            }.distinct().size == channels.size,
            PresetRecipeIssue.ROOT_COUNT,
        )
        channels.forEach { channel ->
            val points = channel.points
            requireRecipe(
                channel.boneId in requiredBones && points.size in MIN_POINTS..MAX_POINTS,
                PresetRecipeIssue.INVALID_ROOT_CHANNEL,
            )
            requireRecipe(
                points.first().progress == 0f && points.last().progress == 1f,
                PresetRecipeIssue.INVALID_ROOT_ENDPOINTS,
            )
            requireRecipe(
                points.all {
                    it.progress.isFinite() && it.progress in 0f..1f && it.x.isFinite() &&
                        it.y.isFinite() &&
                        abs(it.x) <= MAX_TRAVEL && abs(it.y) <= MAX_TRAVEL
                },
                PresetRecipeIssue.INVALID_ROOT_POINT,
            )
            requireRecipe(
                points.zipWithNext().all { (a, b) ->
                    b.progress - a.progress >= MIN_SPACING
                },
                PresetRecipeIssue.UNORDERED_ROOT_POINTS,
            )
            if (loop) {
                requireRecipe(
                    points.first().x == points.last().x && points.first().y == points.last().y,
                    PresetRecipeIssue.ROOT_LOOP_ENDPOINTS,
                )
            }
        }
    }

    private const val MAX_CHANNELS = 16
    private const val MIN_POINTS = 2
    private const val MAX_POINTS = 64
    private const val MAX_TRAVEL = 4f
    private const val MIN_SPACING = .000001f
}
