package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Snaps finished stroke points onto [guide]. A mirror guide appends the reflected stroke. */
internal fun List<TouchPoint>.snapToRuler(guide: RulerGuide?): List<TouchPoint> {
    if (guide == null || guide.mode == RulerMode.NONE || isEmpty()) return this
    return when (guide.mode) {
        RulerMode.LINE -> map { point ->
            val a = guide.start
            val b = guide.end
            val dx = b.x - a.x
            val dy = b.y - a.y
            val t = (((point.x - a.x) * dx + (point.y - a.y) * dy) / (dx * dx + dy * dy).coerceAtLeast(1f))
            point.copy(x = a.x + dx * t, y = a.y + dy * t)
        }

        RulerMode.MIRROR -> this + asReversed().map { point ->
            point.copy(x = guide.center.x - (point.x - guide.center.x))
        }

        RulerMode.CIRCLE -> map { point ->
            val angle = atan2(point.y - guide.center.y, point.x - guide.center.x)
            point.copy(
                x = guide.center.x + cos(angle) * guide.radius.x,
                y = guide.center.y + sin(angle) * guide.radius.y,
            )
        }

        RulerMode.BOX -> map { point ->
            val left = guide.center.x - guide.radius.x
            val right = guide.center.x + guide.radius.x
            val top = guide.center.y - guide.radius.y
            val bottom = guide.center.y + guide.radius.y
            val distances =
                floatArrayOf(abs(point.x - left), abs(point.x - right), abs(point.y - top), abs(point.y - bottom))
            when (distances.indices.minBy { distances[it] }) {
                0 -> point.copy(x = left)
                1 -> point.copy(x = right)
                2 -> point.copy(y = top)
                else -> point.copy(y = bottom)
            }
        }

        RulerMode.NONE -> this
    }
}
