package com.donnclab.kinetiframe.studio.drawing

import com.donnclab.kinetiframe.core.document.DrawCommand
import com.donnclab.kinetiframe.core.document.TouchPoint

/** Decides which strokes a stroke eraser touches. */
internal object StrokeHitTest {
    /** True when any eraser point comes within half the combined widths of a segment of [stroke]. */
    fun intersects(stroke: DrawCommand, eraserPoints: List<TouchPoint>, eraserSize: Float): Boolean {
        val strokePoints = stroke.points
        if (strokePoints.isEmpty()) return false

        val threshold = (stroke.width + eraserSize) / 2f
        val thresholdSq = threshold * threshold

        for (eP in eraserPoints) {
            for (i in 0 until strokePoints.size - 1) {
                val p1 = strokePoints[i]
                val p2 = strokePoints[i + 1]
                if (distToSegmentSquared(eP.x, eP.y, p1.x, p1.y, p2.x, p2.y) < thresholdSq) {
                    return true
                }
            }
        }
        return false
    }

    private fun distToSegmentSquared(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1)
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        return if (t < 0) {
            (px - x1) * (px - x1) + (py - y1) * (py - y1)
        } else if (t > 1) {
            (px - x2) * (px - x2) + (py - y2) * (py - y2)
        } else {
            val projX = x1 + t * dx
            val projY = y1 + t * dy
            (px - projX) * (px - projX) + (py - projY) * (py - projY)
        }
    }
}
