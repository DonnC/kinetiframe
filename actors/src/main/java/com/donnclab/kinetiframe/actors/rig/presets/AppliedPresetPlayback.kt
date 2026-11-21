package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedPresetProgram
import com.donnclab.kinetiframe.engine.rigging.presets.PresetDuration

/*
 * Playback of a saved preset. Schema 2 and 3 recipes evaluate authored curves, with optional exact
 * counter-rotation. Legacy schema 1 recipes evaluate their oscillator: a looping recipe is a pure
 * sinusoid, and a one-shot recipe eases into its held offset while the sinusoid settles to zero
 * (see the engine's PresetRotationProgram).
 */

val AppliedPresetDocument.durationMicros: Long get() = PresetDuration.micros(recipe.durationSeconds, speed)

/** Each bone's rotation offset, in degrees, at [timeMicros]. */
fun AppliedPresetDocument.offsets(timeMicros: Long): Map<String, Float> =
    AppliedPresetPrograms.of(this).offsets(timeMicros)

fun AppliedPresetDocument.rootTranslations(timeMicros: Long, rest: SceneGraph): Map<String, RootTranslation> {
    if (recipe.rootMotion.isEmpty()) return emptyMap()
    return PresetRootMotion.evaluate(AppliedPresetPrograms.of(this), timeMicros, rest)
}

/**
 * Keyed by identity: a saved document is immutable and reused between frames, so playback builds
 * each preset's evaluation program once instead of on every frame.
 */
private object AppliedPresetPrograms {
    private const val CACHE_SIZE = 32

    private val cache = object : LinkedHashMap<IdentityKey, AppliedPresetProgram>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IdentityKey, AppliedPresetProgram>) =
            size > CACHE_SIZE
    }

    fun of(document: AppliedPresetDocument): AppliedPresetProgram = synchronized(cache) {
        cache.getOrPut(IdentityKey(document)) { AppliedPresetProgram(document) }
    }

    private class IdentityKey(val document: AppliedPresetDocument) {
        override fun equals(other: Any?) = other is IdentityKey && other.document === document

        override fun hashCode() = System.identityHashCode(document)
    }
}
