package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.math.Fabrik

/** Product and public sandbox execute the same neutral solver. */
object FabrikSolver {
    fun solve(
        joints: List<Point2>,
        target: Point2,
        iterations: Int = Fabrik.DEFAULT_ITERATIONS,
        tolerance: Float = Fabrik.DEFAULT_TOLERANCE,
    ): List<Point2> = Fabrik.solve(joints, target, iterations, tolerance)
}
