package com.donnclab.kinetiframe.domain.actor

import com.donnclab.kinetiframe.data.actor.RootMotionCurve
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.motion.CurveSampling
import com.donnclab.kinetiframe.engine.rigging.motion.MotionCurveSampler

object RootMotionEvaluator {
    private val sampler = MotionCurveSampler()

    fun sample(curves: List<RootMotionCurve>, time: Long, sampling: CurveSampling): Map<String, RootTranslation> =
        sampler.rootTranslations(curves, time, sampling)
}
