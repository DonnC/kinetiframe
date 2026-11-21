package com.donnclab.kinetiframe.presentation.studio.canvas

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.RulerMode

internal fun nearestGuideHandle(point: Point2, guide: RulerGuide): Int {
    val handles = when (guide.mode) {
        RulerMode.LINE, RulerMode.MIRROR -> listOf(guide.start, guide.end, guide.center)

        RulerMode.CIRCLE, RulerMode.BOX -> listOf(
            Point2(guide.center.x - guide.radius.x, guide.center.y),
            Point2(guide.center.x + guide.radius.x, guide.center.y),
            Point2(guide.center.x, guide.center.y - guide.radius.y),
            Point2(guide.center.x, guide.center.y + guide.radius.y),
            guide.center,
        )

        RulerMode.NONE -> emptyList()
    }
    return handles.indexOfFirst { (it - point).getDistance() < 42f }
}

internal fun moveGuide(guide: RulerGuide, handle: Int, delta: Point2): RulerGuide = when (guide.mode) {
    RulerMode.LINE, RulerMode.MIRROR -> when (handle) {
        0 -> guide.withAxis(start = guide.start + delta, end = guide.end)
        1 -> guide.withAxis(start = guide.start, end = guide.end + delta)
        else -> guide.copy(start = guide.start + delta, end = guide.end + delta, center = guide.center + delta)
    }

    RulerMode.CIRCLE, RulerMode.BOX -> when (handle) {
        0 -> guide.copy(radius = guide.radius.copy(x = (guide.radius.x - delta.x).coerceAtLeast(20f)))
        1 -> guide.copy(radius = guide.radius.copy(x = (guide.radius.x + delta.x).coerceAtLeast(20f)))
        2 -> guide.copy(radius = guide.radius.copy(y = (guide.radius.y - delta.y).coerceAtLeast(20f)))
        3 -> guide.copy(radius = guide.radius.copy(y = (guide.radius.y + delta.y).coerceAtLeast(20f)))
        else -> guide.copy(center = guide.center + delta)
    }

    RulerMode.NONE -> guide
}

/** Keeps the visible centre handle aligned with an edited line or mirror axis. */
private fun RulerGuide.withAxis(start: Point2, end: Point2): RulerGuide = copy(
    start = start,
    end = end,
    center = Point2((start.x + end.x) / 2f, (start.y + end.y) / 2f),
)
