package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.engine.rigging.math.CompiledMotionCurve
import com.donnclab.kinetiframe.engine.rigging.motion.MotionInterpolation

fun CurveInterpolation.engineInterpolation(): CompiledMotionCurve.Interpolation = MotionInterpolation.curve(this)
