package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedPresetProgram
import com.donnclab.kinetiframe.engine.rigging.presets.PresetRootMotionSampler

internal object PresetRootMotion {
    private val sampler = PresetRootMotionSampler()

    fun evaluate(program: AppliedPresetProgram, time: Long, rest: SceneGraph): Map<String, RootTranslation> =
        program.rootTranslations(time, rest, sampler)
}
