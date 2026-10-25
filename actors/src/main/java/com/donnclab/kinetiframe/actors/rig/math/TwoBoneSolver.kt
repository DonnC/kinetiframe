package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.BendLimits
import com.donnclab.kinetiframe.engine.rigging.math.BendSide
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneException
import com.donnclab.kinetiframe.engine.rigging.math.TwoBoneIk

object TwoBoneSolver {
    fun solve(
        joints: List<Offset>,
        target: Offset,
        bendSide: Int,
        limits: BendLimits = BendLimits.Default,
    ): List<Offset> {
        require(joints.size == 3 && bendSide in listOf(-1, 1))
        val side = if (bendSide < 0) BendSide.NEGATIVE else BendSide.POSITIVE
        return try {
            TwoBoneIk.solve(joints.map { Point2(it.x, it.y) }, Point2(target.x, target.y), side, limits)
                .map { Offset(it.x, it.y) }
        } catch (failure: TwoBoneException) {
            throw IllegalArgumentException("Failed requirement.")
        }
    }

    fun bendSide(joints: List<Offset>): Int = TwoBoneIk.bendSide(joints.map { Point2(it.x, it.y) }).sign
}
