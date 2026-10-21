package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.math.Fabrik

/** Compatibility adapter only. Product and public sandbox execute the same neutral solver. */
object FabrikSolver {
    fun solve(
        joints: List<Offset>,
        target: Offset,
        iterations: Int = Fabrik.DEFAULT_ITERATIONS,
        tolerance: Float = Fabrik.DEFAULT_TOLERANCE,
    ): List<Offset> = Fabrik.solve(joints.map { Point2(it.x, it.y) }, Point2(target.x, target.y), iterations, tolerance)
        .map { Offset(it.x, it.y) }
}
