package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode

/** Single UI-to-engine boundary for persistent ruler geometry. */
internal fun RulerGuide.toConstraintSpec(): RulerConstraintSpec = RulerConstraintSpec(
    mode = when (mode) {
        RulerMode.NONE -> RulerConstraintMode.NONE
        RulerMode.LINE -> RulerConstraintMode.LINE
        RulerMode.CIRCLE -> RulerConstraintMode.ELLIPSE
        RulerMode.BOX -> RulerConstraintMode.RECTANGLE
        RulerMode.MIRROR -> RulerConstraintMode.MIRROR
    },
    startX = start.x,
    startY = start.y,
    endX = end.x,
    endY = end.y,
    centerX = center.x,
    centerY = center.y,
    radiusX = radius.x,
    radiusY = radius.y,
    rotationDegrees = rotation,
)
