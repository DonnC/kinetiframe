package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawingRegion
import kotlin.math.hypot
import kotlin.math.min

/** Corrections feather inside their lasso, preserving shared joint vertices at the boundary.
 * Later lassos win in overlap. Keep at most four influences after blending, not five. */
object DrawingRegionWeights {
    fun influence(region: ActorDrawingRegion, x: Float, y: Float, width: Int, height: Int): Float {
        var inside = false
        var distance = Float.POSITIVE_INFINITY
        val points = region.outline
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val ax = a.x * width
            val ay = a.y * height
            val bx = b.x * width
            val by = b.y * height
            if ((ay > y) != (by > y) && x < (bx - ax) * (y - ay) / (by - ay) + ax) inside = !inside
            val dx = bx - ax
            val dy = by - ay
            val length = dx * dx + dy * dy
            val t = if (length > 0f) (((x - ax) * dx + (y - ay) * dy) / length).coerceIn(0f, 1f) else 0f
            distance = min(distance, hypot(x - ax - dx * t, y - ay - dy * t))
        }
        if (!inside) return 0f
        if (region.featherPixels == 0f) return 1f
        val t = (distance / region.featherPixels).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }
    fun blend(weights: List<Float>, boneIndex: Int, alpha: Float): List<Float> {
        if (alpha <= 0f) return weights
        val mixed = weights.mapIndexed { i, w -> w * (1f - alpha) + if (i == boneIndex) alpha else 0f }
        val keep = mixed.indices.sortedByDescending { mixed[it] }.take(4).toSet()
        val total = mixed.indices.filter { it in keep }.sumOf { mixed[it].toDouble() }.toFloat()
        return mixed.mapIndexed { i, w -> if (i in keep) w / total else 0f }
    }
}
