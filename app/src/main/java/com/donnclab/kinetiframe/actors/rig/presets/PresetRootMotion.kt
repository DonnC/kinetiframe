package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.actors.rig.model.RootTranslation
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedPresetProgram
import com.donnclab.kinetiframe.engine.rigging.presets.PresetRootMotionSampler

typealias RootMotionPoint = com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint

typealias RootMotionChannel = com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel

internal object PresetRootMotion {
    private val sampler = PresetRootMotionSampler()

    fun evaluate(program: AppliedPresetProgram, time: Long, rest: SceneGraph): Map<String, RootTranslation> =
        program.rootTranslations(time, rest, sampler)
}
