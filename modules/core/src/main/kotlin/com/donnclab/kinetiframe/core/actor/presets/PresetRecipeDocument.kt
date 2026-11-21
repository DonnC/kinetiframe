// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.presets

import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import kotlinx.serialization.Serializable

/**
 * A motion recipe as shipped in the preset assets and snapshotted into saved performances, so later
 * asset updates can't change an existing animation.
 *
 * Schema 1 keeps the legacy oscillator, schema 2 carries authored, shape-preserving curves and
 * schema 3 adds authored root translation. Saved snapshots keep their schema and joint values.
 */
@Serializable(with = PresetRecipeDocumentSerializerImpl::class)
data class PresetRecipeDocument(
    val recipe: PresetRecipe,
    val description: String,
    /** Shipping human recipes were authored front-facing; matching role IDs alone is insufficient. */
    val authoredView: ActorView = ActorView.FRONT,
) {
    val id: String get() = recipe.metadata.id
    val name: String get() = recipe.metadata.name
    val schemaVersion: Int get() = recipe.metadata.schemaVersion
    val durationSeconds: Float get() = recipe.metadata.durationSeconds

    /**
     * True for a repeating gesture (idle, walk, talk); false for a one-shot transition that plays
     * once and holds its final pose (sit down, jump, surprise).
     */
    val loop: Boolean get() = recipe.loop
    val requiredBones: List<String> get() = recipe.channels.requiredBones
    val rotations: List<PresetRotationChannel> get() = recipe.channels.rotations
    val rootMotion: List<RootMotionChannel> get() = recipe.channels.roots

    /** Throws the typed validation failure for the first problem. */
    fun validate(): PresetRecipeDocument = apply { PresetRecipeValidation.requireValid(recipe) }

    fun missingBones(bones: Set<String>): List<String> = requiredBones.filterNot(bones::contains)

    /** A recipe plays on the view it was drawn for, and on the original drawing, which has no view. */
    fun supportsView(view: ActorView): Boolean = view == ActorView.ORIGINAL || view == authoredView
}
