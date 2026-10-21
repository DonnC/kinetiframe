package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph

typealias RootMotionPoint = com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint

typealias RootMotionChannel = com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel

internal object PresetRootMotion {
    private val sampler = PresetRootMotionSampler()

    fun evaluate(program: AppliedPresetProgram, time: Long, rest: SceneGraph): Map<String, RootTranslation> =
        program.rootTranslations(time, rest, sampler)
}
