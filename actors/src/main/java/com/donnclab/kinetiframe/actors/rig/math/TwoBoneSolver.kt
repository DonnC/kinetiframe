package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.BendLimits
import com.donnclab.kinetiframe.engine.rigging.math.BendSide
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneException
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk

object TwoBoneSolver {
    fun solve(
        joints: List<Point2>,
        target: Point2,
        bendSide: Int,
        limits: BendLimits = BendLimits.Default,
    ): List<Point2> {
        require(joints.size == 3 && bendSide in listOf(-1, 1))
        val side = if (bendSide < 0) BendSide.NEGATIVE else BendSide.POSITIVE
        return try {
            TwoBoneIk.solve(joints, target, side, limits)
        } catch (failure: TwoBoneException) {
            throw IllegalArgumentException("Failed requirement.")
        }
    }

    fun bendSide(joints: List<Point2>): Int = TwoBoneIk.bendSide(joints).sign
}
