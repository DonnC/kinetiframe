package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import com.donnclab.kinetiframe.engine.rigging.motion.MotionCurveSampler

object RootMotionEvaluator {
    private val sampler = MotionCurveSampler()

    fun sample(curves: List<RootMotionCurve>, time: Long, sampling: CurveSampling): Map<String, RootTranslation> =
        sampler.rootTranslations(curves, time, sampling)
}
