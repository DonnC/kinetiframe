// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import kotlin.math.abs

object PresetChannelValidation {
    fun requireValid(channels: List<PresetRotationChannel>, schemaVersion: Int, loop: Boolean) {
        channels.forEach { channel ->
            check(channel.boneId.isNotBlank(), PresetChannelIssue.EMPTY_PART_ID)
            val controls = channel.oscillation
            check(
                listOf(
                    controls.offsetDegrees,
                    controls.amplitudeDegrees,
                    controls.cyclesPerLoop,
                    controls.phaseDegrees,
                ).all(Float::isFinite),
                PresetChannelIssue.NONFINITE_CONTROLS,
            )
            check(
                controls.cyclesPerLoop in 0f..MAX_CYCLES && abs(controls.amplitudeDegrees) <= HALF_TURN,
                PresetChannelIssue.INVALID_OSCILLATION,
            )
            check(
                abs(controls.offsetDegrees) <= FULL_TURN && abs(controls.phaseDegrees) <= FULL_TURN,
                PresetChannelIssue.INVALID_PHASE_OFFSET,
            )
            if (schemaVersion ==
                LEGACY_SCHEMA
            ) {
                check(
                    channel.points.isEmpty() && channel.counterRotationBones.isEmpty(),
                    PresetChannelIssue.LEGACY_AUTHORED_FIELDS,
                )
            }
            if (schemaVersion >= AUTHORED_SCHEMA) validateAuthored(channel, channels, loop)
        }
    }

    private fun validateAuthored(channel: PresetRotationChannel, channels: List<PresetRotationChannel>, loop: Boolean) {
        val points = channel.points
        check(points.size in MIN_POINTS..MAX_POINTS, PresetChannelIssue.POINT_LIMIT)
        check(points.first().progress == 0f && points.last().progress == 1f, PresetChannelIssue.INVALID_ENDPOINTS)
        check(
            points.all {
                it.progress.isFinite() && it.progress in 0f..1f && it.degrees.isFinite() &&
                    abs(it.degrees) <= HALF_TURN
            },
            PresetChannelIssue.INVALID_POINT,
        )
        check(points.zipWithNext().all { (a, b) -> b.progress > a.progress }, PresetChannelIssue.UNORDERED_POINTS)
        if (loop) check(points.first().degrees == points.last().degrees, PresetChannelIssue.LOOP_ENDPOINTS)
        check(
            channel.counterRotationBones.distinct().size == channel.counterRotationBones.size,
            PresetChannelIssue.DUPLICATE_COMPENSATION,
        )
        check(
            channel.counterRotationBones.all { id ->
                id != channel.boneId &&
                    channels.any { it.boneId == id && it.counterRotationBones.isEmpty() }
            },
            PresetChannelIssue.INVALID_COMPENSATION,
        )
    }

    private fun check(valid: Boolean, issue: PresetChannelIssue) {
        if (!valid) throw PresetChannelValidationException(issue)
    }

    private const val LEGACY_SCHEMA = 1
    private const val AUTHORED_SCHEMA = 2
    private const val MIN_POINTS = 2
    private const val MAX_POINTS = 64
    private const val MAX_CYCLES = 20f
    private const val HALF_TURN = 180f
    private const val FULL_TURN = 360f
}

enum class PresetChannelIssue {
    EMPTY_PART_ID,
    NONFINITE_CONTROLS,
    INVALID_OSCILLATION,
    INVALID_PHASE_OFFSET,
    LEGACY_AUTHORED_FIELDS,
    POINT_LIMIT,
    INVALID_ENDPOINTS,
    INVALID_POINT,
    UNORDERED_POINTS,
    LOOP_ENDPOINTS,
    DUPLICATE_COMPENSATION,
    INVALID_COMPENSATION,
}

class PresetChannelValidationException(val issue: PresetChannelIssue) : IllegalArgumentException(issue.name)
