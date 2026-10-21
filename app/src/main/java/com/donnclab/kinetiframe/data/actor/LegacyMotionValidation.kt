package com.donnclab.kinetiframe.data.actor

import com.donnclab.kinetiframe.core.actor.motion.MotionValidationException
import com.donnclab.kinetiframe.core.actor.motion.MotionValidationIssue

/** Preserve product diagnostics while public models expose typed, presentation-neutral errors. */
fun RotationCurve.validate(durationMicros: Long): RotationCurve = apply {
    legacyValidation(MotionChannelKind.ROTATION) { requireValid(durationMicros) }
}

fun RootMotionCurve.validate(durationMicros: Long): RootMotionCurve = apply {
    legacyValidation(MotionChannelKind.ROOT) { requireValid(durationMicros) }
}

private enum class MotionChannelKind { ROTATION, ROOT }

private fun legacyValidation(kind: MotionChannelKind, validate: () -> Unit) {
    try {
        validate()
    } catch (failure: MotionValidationException) {
        throw IllegalArgumentException(legacyMessage(failure.issue, kind), failure)
    }
}

private fun legacyMessage(issue: MotionValidationIssue, kind: MotionChannelKind): String = when (issue) {
    MotionValidationIssue.EMPTY_PART_ID -> when (kind) {
        MotionChannelKind.ROTATION -> "Curve needs a part ID"
        MotionChannelKind.ROOT -> "Root motion needs a part ID"
    }

    MotionValidationIssue.EMPTY_POINTS -> "Curve needs points"

    MotionValidationIssue.POINT_LIMIT -> "Curve has too many points"

    MotionValidationIssue.UNORDERED_POINTS -> when (kind) {
        MotionChannelKind.ROTATION -> "Curve points must be ordered uniquely"
        MotionChannelKind.ROOT -> "Root points must be ordered uniquely"
    }

    MotionValidationIssue.INVALID_ROTATION_POINT -> "Invalid curve point"

    MotionValidationIssue.ROOT_POINT_BUDGET -> "Root motion point budget exceeded"

    MotionValidationIssue.INVALID_ROOT_POINT -> "Invalid root motion point"
}
