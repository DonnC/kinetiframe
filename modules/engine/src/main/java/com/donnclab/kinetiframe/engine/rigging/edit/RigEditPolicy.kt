// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.BendSide
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk

enum class RootMoveScope { SELECTED, ALL }
enum class PoseBend { AUTOMATIC, NEGATIVE, POSITIVE, INVALID }
enum class PoseEditIssue { INVALID_BEND }
class PoseEditException(val issue: PoseEditIssue) : IllegalArgumentException(issue.name)

internal fun PoseBend.resolve(points: List<Point2>): BendSide = when (this) {
    PoseBend.AUTOMATIC -> TwoBoneIk.bendSide(points)

    PoseBend.NEGATIVE -> BendSide.NEGATIVE

    PoseBend.POSITIVE -> BendSide.POSITIVE

    // The legacy boundary rejects bad signs only when an analytic endpoint actually consumes them.
    PoseBend.INVALID -> throw PoseEditException(PoseEditIssue.INVALID_BEND)
}

internal object RigEditLimits {
    const val ATTACHMENT_FIT_EPSILON = .1f
    const val POINT_EPSILON = .001f
    const val MIN_FIT_LENGTH = 1f
}
