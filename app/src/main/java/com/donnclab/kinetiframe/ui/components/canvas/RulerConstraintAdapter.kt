package com.donnclab.kinetiframe.ui.components.canvas

import com.donnclab.kinetiframe.engine.ruler.RulerConstraintMode
import com.donnclab.kinetiframe.engine.ruler.RulerConstraintSpec
import com.donnclab.kinetiframe.ui.RulerGuide
import com.donnclab.kinetiframe.ui.RulerMode

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
