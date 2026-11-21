// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.presets

import com.donnclab.kinetiframe.core.actor.motion.PresetClockMode
import com.donnclab.kinetiframe.core.actor.motion.PresetTiming
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.presets.LegacyPresetSampling
import com.donnclab.kinetiframe.core.actor.presets.PresetRootSampling
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Supply immutable recipe revisions; root cache lifetime belongs to the caller, not each program. */
class AppliedPresetProgram(private val document: AppliedPresetDocument) {
    private val recipe = document.recipe.recipe
    private val mode = if (recipe.loop) PresetClockMode.REPEAT else PresetClockMode.ONE_SHOT
    private val rotation by lazy { PresetRotationProgram(recipe.channels.rotations) }

    val durationMicros: Long get() = PresetDuration.micros(recipe.metadata.durationSeconds, document.speed)

    fun offsets(timeMicros: Long): Map<String, Float> = if (recipe.metadata.schemaVersion >= AUTHORED_SCHEMA) {
        rotation.authored(timeMicros, PresetTiming(durationMicros, mode), document.intensity)
    } else {
        rotation.legacy(
            timeMicros,
            LegacyPresetSampling(recipe.metadata.durationSeconds, document.speed, document.intensity, mode),
        )
    }

    fun rootTranslations(
        timeMicros: Long,
        rest: SceneGraph,
        sampler: PresetRootMotionSampler,
    ): Map<String, RootTranslation> {
        if (recipe.channels.roots.isEmpty()) return emptyMap()
        return sampler.evaluate(
            recipe.channels.roots,
            timeMicros,
            rest,
            PresetRootSampling(durationMicros, recipe.loop, document.intensity),
        )
    }

    companion object {
        private const val AUTHORED_SCHEMA = 2
    }
}
