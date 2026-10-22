package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.actors.rig.model.ActorView
import com.donnclab.kinetiframe.actors.rig.model.RootTranslation
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.engine.rigging.presets.AppliedPresetProgram
import com.donnclab.kinetiframe.engine.rigging.presets.PresetCurve
import com.donnclab.kinetiframe.engine.rigging.presets.PresetDuration
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Versioned asset contract. A channel describes continuous motion in normalized clip time.
 *
 * Schema 1 retains the legacy oscillator; schema 2 carries authored, shape-preserving curves.
 * Existing saved recipe snapshots retain their schema and joint values.
 */
@Serializable(with = ActorMotionPresetSerializerImpl::class)
data class ActorMotionPreset(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val description: String,
    val durationSeconds: Float,
    val requiredBones: List<String>,
    val channels: List<RotationChannel>,
    /** True (default) for a repeating gesture (idle, walk, talk); false for a one-shot transition
     *  that plays once and holds its final pose (sit down, jump, surprise) -- see
     *  [AppliedActorPreset.offsets]. */
    val loop: Boolean = true,
    /** Resolved from the paired asset filename, never from a recipe ID or exported math. */
    @Transient val previewAssetPath: String? = null,
    /** Schema 3: scale-aware, authored pelvis/flight translation, not random procedural jitter. */
    val rootMotion: List<RootMotionChannel> = emptyList(),
    /** Shipping human recipes were authored front-facing; matching role IDs alone is insufficient. */
    val authoredView: ActorView = ActorView.FRONT,
) {
    fun validate(): ActorMotionPreset = apply {
        validatePresetRecipe(this)
    }

    fun missingBones(bones: Set<String>): List<String> = requiredBones.filterNot(bones::contains)
    fun supportsView(view: ActorView): Boolean = authoredView.neutral().supportsView(view.neutral())
}

@Serializable(with = RotationChannelSerializerImpl::class)
data class RotationChannel(
    val boneId: String,
    val offsetDegrees: Float = 0f,
    val amplitudeDegrees: Float = 0f,
    val cyclesPerLoop: Float = 1f,
    val phaseDegrees: Float = 0f,
    /** Schema 2: authored local rotation offsets, not absolute rest angles. */
    val points: List<RotationPoint> = emptyList(),
    /** Subtract these evaluated local parent offsets to preserve a descendant's world angle. */
    val counterRotationBones: List<String> = emptyList(),
)

typealias RotationPoint = com.donnclab.kinetiframe.core.actor.presets.RotationPoint

/**
 * Store the recipe snapshot so future asset-pack updates cannot change an existing animation.
 *
 * Schema 2 evaluates authored curves through [PresetCurve], with optional exact counter-rotation.
 * The following two models apply only to legacy schema-1 snapshots:
 *
 * **Looping** (`recipe.loop == true`, the original and still-default model): every channel is a
 * pure sinusoid running forever -- `offsetDegrees + amplitudeDegrees * sin(phase)`, where `phase`
 * grows without bound as `timeMicros` advances. This suits a continuously repeating gesture
 * (breathing, walking, talking, a wagging tail): there is no "end," just a stable cycle.
 *
 * **One-shot** (`recipe.loop == false`): a repeating sinusoid is the wrong shape for something
 * that happens once and then stops -- a walk cycle repeats forever, but nobody sits down twice.
 * Real single transitions (sitting into a chair, landing a jump, a startled flinch) share a
 * common structure biomechanically: the body moves toward a resting configuration while a brief
 * transient -- momentum carrying past the target, or the joints absorbing an impact -- decays
 * away as the motion settles, the way a lightly-damped spring overshoots and then rests at
 * equilibrium rather than snapping straight there. This function reproduces exactly that shape
 * with the same two knobs the schema already has, no new authoring concepts required:
 *
 * - `progress` is clip time normalized to `[0, 1]` and clamped there, so the transition plays
 *   once and then holds -- it never wraps back to zero the way the looping model's phase does.
 * - `eased = smoothstep(progress)` (the standard `3x^2 - 2x^3` ease used throughout animation and
 *   graphics for its zero velocity at both ends) drives `offsetDegrees` from 0 up to its full
 *   value: this is the channel settling into its held final pose.
 * - `settle = 1 - eased` decays from 1 to 0 over the same span and multiplies the sinusoidal
 *   `amplitudeDegrees * sin(...)` term: this is the transient overshoot/absorb wobble, present at
 *   the start and mathematically guaranteed to reach exactly zero by `progress == 1` regardless of
 *   `cyclesPerLoop`/`phaseDegrees`, so a channel's final held value is always precisely
 *   `offsetDegrees` -- `cyclesPerLoop`/`phaseDegrees` only shape the transient's arc along the way,
 *   never the endpoint. A large `phaseDegrees` (90 is used throughout the bundled one-shot
 *   recipes) puts the transient at its peak at `progress == 0`, so the clip visibly starts from a
 *   displaced pose (e.g. mid-air, crouched) and eases into the held target.
 */
@Serializable(with = AppliedActorPresetSerializerImpl::class)
data class AppliedActorPreset(val recipe: ActorMotionPreset, val intensity: Float = 1f, val speed: Float = 1f) {
    val durationMicros: Long get() = PresetDuration.micros(recipe.durationSeconds, speed)

    private val program by lazy {
        AppliedPresetProgram(AppliedPresetDocument(recipe.document(), intensity, speed))
    }

    fun offsets(timeMicros: Long): Map<String, Float> = program.offsets(timeMicros)

    fun rootTranslations(timeMicros: Long, rest: SceneGraph): Map<String, RootTranslation> {
        if (recipe.rootMotion.isEmpty()) return emptyMap()
        return PresetRootMotion.evaluate(program, timeMicros, rest)
    }
}
