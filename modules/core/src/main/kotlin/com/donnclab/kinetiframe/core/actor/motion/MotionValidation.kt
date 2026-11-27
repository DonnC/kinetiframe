// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.motion

enum class MotionValidationIssue {
    EMPTY_PART_ID,
    EMPTY_POINTS,
    POINT_LIMIT,
    UNORDERED_POINTS,
    INVALID_ROTATION_POINT,
    ROOT_POINT_BUDGET,
    INVALID_ROOT_POINT,
}

/** Machine-readable reason; presentation maps this to localized or legacy diagnostics. */
class MotionValidationException(val issue: MotionValidationIssue) : IllegalArgumentException(issue.name)

internal fun requireMotion(valid: Boolean, issue: MotionValidationIssue) {
    if (!valid) throw MotionValidationException(issue)
}
