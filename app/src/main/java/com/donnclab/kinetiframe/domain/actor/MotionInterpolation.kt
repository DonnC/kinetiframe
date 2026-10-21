package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.CurveInterpolation
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.MotionInterpolation

fun CurveInterpolation.engineInterpolation(): CompiledMotionCurve.Interpolation = MotionInterpolation.curve(this)
