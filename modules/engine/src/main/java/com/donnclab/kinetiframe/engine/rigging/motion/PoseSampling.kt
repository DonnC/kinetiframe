// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve

data class PoseSampling(
    val loop: Boolean = false,
    val interpolation: CompiledMotionCurve.Interpolation = CompiledMotionCurve.Interpolation.SMOOTH,
)
