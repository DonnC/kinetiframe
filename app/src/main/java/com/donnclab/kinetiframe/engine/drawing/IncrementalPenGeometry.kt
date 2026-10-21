package com.donnclab.kinetiframe.engine.drawing

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.donnclab.kinetiframe.engine.model.TouchPoint

/** Gesture-owned geometry. No document-sized raster surface and no growing prediction-list copy. */
class IncrementalPenGeometry {
    private val stable = Path()
    private val preview = Path()
    private var stableSegments = 0

    fun draw(canvas: Canvas, points: List<TouchPoint>, width: Float, paint: Paint) {
        if (points.isEmpty()) return
        if (points.size < stableSegments + 1) {
            reset()
        }
        if (stableSegments == 0) {
            stable.rewind()
            stable.moveTo(points[0].x, points[0].y)
        }
        while (stableSegments < points.lastIndex) {
            PenStrokeRenderer.appendSegment(stable, points, stableSegments)
            stableSegments++
        }
        preview.set(stable)
        PenStrokeRenderer.finishTip(preview, points.last())
        // One alpha deposit across prefix, mutable tip, joins and all self-intersections.
        PenStrokeRenderer.drawPath(canvas, preview, points, width, paint)
    }

    fun reset() {
        stable.rewind()
        preview.rewind()
        stableSegments = 0
    }
}
