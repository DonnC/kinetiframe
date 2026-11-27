// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve

object MotionInterpolation {
    fun curve(mode: CurveInterpolation): CompiledMotionCurve.Interpolation = when (mode) {
        CurveInterpolation.SMOOTH -> CompiledMotionCurve.Interpolation.SMOOTH
        CurveInterpolation.EASE_IN_OUT -> CompiledMotionCurve.Interpolation.EASE_IN_OUT
        CurveInterpolation.LINEAR -> CompiledMotionCurve.Interpolation.LINEAR
        CurveInterpolation.STEP -> CompiledMotionCurve.Interpolation.STEP
    }
}
