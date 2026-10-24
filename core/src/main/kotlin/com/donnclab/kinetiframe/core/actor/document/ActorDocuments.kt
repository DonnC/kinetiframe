// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.document

import com.donnclab.kinetiframe.core.actor.artwork.ActorArtworkKey
import com.donnclab.kinetiframe.core.actor.artwork.SceneArtwork
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.CurveInterpolation
import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.presets.AppliedPresetDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetValidationDiagnostics
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.core.document.StorageIds
import com.donnclab.kinetiframe.core.geometry.Affine2D
import kotlinx.serialization.Serializable

/** Stable actor identity and its currently editable structural revision. */
@Serializable
data class ActorManifest(
    val schemaVersion: Int = SCHEMA_VERSION,
    val actorId: String,
    val displayName: String,
    val templateId: String,
    val currentRevisionId: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    /** Empty on legacy actors. The editable pointer selects one authored view; motion pins revisions. */
    val viewRevisions: Map<ActorView, String> = emptyMap(),
) {
    fun authoredViews() = viewRevisions.ifEmpty {
        mapOf(ActorView.ORIGINAL to currentRevisionId)
    }

    fun validate(): ActorManifest = apply {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        ActorDocumentValidation.requireId(currentRevisionId, "revision ID")
        require(viewRevisions.isEmpty() || currentRevisionId in viewRevisions.values) {
            "Editable revision must belong to an authored view"
        }
        require(viewRevisions.values.distinct().size == viewRevisions.size) { "Views cannot share a revision" }
        viewRevisions.values.forEach { ActorDocumentValidation.requireId(it, "view revision ID") }
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported actor schema $schemaVersion" }
        require(displayName.isNotBlank() && displayName.length <= 60) { "Actor name must contain 1–60 characters" }
        require(templateId in VALID_TEMPLATE_IDS) { "Unsupported actor template '$templateId'" }
        require(createdAtEpochMs >= 0 && updatedAtEpochMs >= createdAtEpochMs) { "Invalid actor timestamps" }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val HUMAN_TEMPLATE_ID = "human-v1"
        const val ANIMAL_TEMPLATE_ID = "animal-v1"

        /** A blank single-root rig the animator builds bone-by-bone from scratch in Actor Studio --
         *  the actor templates hold the minimal starting graph. */
        const val CUSTOM_TEMPLATE_ID = "custom-v1"
        val VALID_TEMPLATE_IDS = setOf(HUMAN_TEMPLATE_ID, ANIMAL_TEMPLATE_ID, CUSTOM_TEMPLATE_ID)
    }
}

/** Immutable setup snapshot. Part IDs in [sceneGraph] remain system-owned. */
@Serializable
data class ActorRevision(
    val schemaVersion: Int = SCHEMA_VERSION,
    val revisionId: String,
    val actorId: String,
    val sceneGraph: SceneGraph,
    val artworkAssetPaths: Map<String, String> = emptyMap(),
    val createdAtEpochMs: Long,
    val view: ActorView = ActorView.ORIGINAL,
) {
    fun validate(): ActorRevision = apply {
        ActorDocumentValidation.requireId(revisionId, "revision ID")
        ActorDocumentValidation.requireId(actorId, "actor ID")
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported actor revision schema $schemaVersion" }
        require(createdAtEpochMs >= 0) { "Invalid revision timestamp" }
        ActorGraphValidation.validate(sceneGraph)
        require(sceneGraph.rootBoneIds.isNotEmpty()) { "Actor revision must have a root part" }
        require(sceneGraph.bones.keys.containsAll(sceneGraph.rootBoneIds)) { "Actor revision has unknown root part" }
        val validArtworkAddresses =
            sceneGraph.bones.keys + SceneArtwork.bindings(sceneGraph).keys
        // Legacy revisions may retain source artwork for an unbound part. Preserve that contract.
        require(artworkAssetPaths.keys.all(validArtworkAddresses::contains)) {
            "Artwork points to an unknown actor part or variant"
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** Project-owned mathematical motion. It always pins the actor revision it was authored for. */
@Serializable
data class PerformanceDocument(
    val schemaVersion: Int = SCHEMA_VERSION,
    val performanceId: String,
    val projectId: String,
    val actorId: String,
    val actorRevisionId: String,
    val displayName: String,
    val durationMicros: Long,
    val playback: PerformancePlayback = PerformancePlayback.LOOP,
    val poseKeys: List<ActorPoseKey> = emptyList(),
    val rotationCurves: List<RotationCurve> = emptyList(),
    /** Exact snapshot of a legacy procedural recipe; never resolved from a mutable asset pack. */
    val presetSnapshot: AppliedPresetDocument? = null,
    val poseInterpolation: CurveInterpolation = CurveInterpolation.SMOOTH,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val artworkKeys: List<ActorArtworkKey> = emptyList(),
    val rootMotionCurves: List<RootMotionCurve> = emptyList(),
) {
    fun validate(): PerformanceDocument = apply {
        ActorDocumentValidation.requireId(performanceId, "performance ID")
        ActorDocumentValidation.requireId(projectId, "project ID")
        ActorDocumentValidation.requireId(actorId, "actor ID")
        ActorDocumentValidation.requireId(actorRevisionId, "actor revision ID")
        require(schemaVersion in 1..SCHEMA_VERSION) { "Unsupported performance schema $schemaVersion" }
        require(displayName.isNotBlank() && displayName.length <= 60) {
            "Performance name must contain 1–60 characters"
        }
        require(durationMicros in 1..PerformanceLimits.MAX_DURATION_MICROS) {
            "Performance duration must be between 1 microsecond and 2 minutes"
        }
        require(
            poseKeys.zipWithNext().all { (a, b) ->
                a.timeMicros < b.timeMicros
            },
        ) { "Pose keys must be ordered uniquely" }
        require(
            poseKeys.all {
                it.timeMicros in 0..durationMicros && it.rotations.values.all(Float::isFinite)
            },
        ) { "Invalid pose key" }
        require(poseKeys.size <= RotationCurve.MAX_POINTS) { "Too many pose keys" }
        require(
            artworkKeys.size <= RotationCurve.MAX_POINTS &&
                artworkKeys.zipWithNext().all { (a, b) ->
                    a.timeMicros < b.timeMicros
                },
        ) { "Artwork keys must be ordered uniquely and bounded" }
        require(
            artworkKeys.all {
                it.timeMicros in 0..durationMicros && it.selections.size <= 64 &&
                    it.selections.all { (part, variant) ->
                        part.isNotBlank() &&
                            (variant.isEmpty() || variant.matches(Regex("[A-Za-z0-9_-]{1,80}")))
                    }
            },
        ) { "Invalid artwork key" }
        poseKeys.forEach { key ->
            require(
                key.rootTranslations.size <= 16 && key.rootTranslations.values.all {
                    it.x.isFinite() && it.y.isFinite() && kotlin.math.abs(it.x) <= 1_000_000 &&
                        kotlin.math.abs(it.y) <= 1_000_000
                },
            ) { "Invalid root motion" }
            require(
                key.contacts.size <= 16 && key.contacts.values.all {
                    it.x.isFinite() && it.y.isFinite() && kotlin.math.abs(it.x) <= 1_000_000 &&
                        kotlin.math.abs(it.y) <= 1_000_000 &&
                        it.bendSide in listOf(-1, 1) && it.followWorldAngle.isFinite()
                },
            ) { "Invalid joint contact" }
        }
        require(
            rotationCurves.map {
                it.boneId
            }.distinct().size == rotationCurves.size,
        ) { "Duplicate rotation curve part" }
        require(rotationCurves.size <= MAX_ROTATION_CURVES) { "Performance has too many rotation curves" }
        rotationCurves.forEach { it.validate(durationMicros) }
        require(
            rootMotionCurves.size <= 16 && rootMotionCurves.map {
                it.rootId
            }.distinct().size == rootMotionCurves.size,
        ) { "Invalid root motion channels" }
        rootMotionCurves.forEach { it.validate(durationMicros) }
        presetSnapshot?.let { preset ->
            PresetValidationDiagnostics.requireValidRecipe(preset.recipe.recipe)
            require(preset.intensity.isFinite() && preset.intensity in 0f..2f) { "Invalid preset intensity" }
            require(preset.speed.isFinite() && preset.speed in 0.25f..3f) { "Invalid preset speed" }
        }
        require(createdAtEpochMs >= 0 && updatedAtEpochMs >= createdAtEpochMs) { "Invalid performance timestamps" }
    }

    companion object {
        const val SCHEMA_VERSION = 7

        /** 64 channels × 720 points bounds a full 30-second high-density import. */
        const val MAX_ROTATION_CURVES = 64
    }
}

/** A FxF reference block. It intentionally contains no duplicated rig or motion data. */
@Serializable
data class ActorPlacement(
    val schemaVersion: Int = SCHEMA_VERSION,
    val placementId: String,
    val projectId: String,
    val performanceId: String,
    val startFrame: Int,
    val durationFrames: Int,
    val repeatCount: Int = 1,
    val transform: Transform = Transform(),
    val zIndex: Int = 0,
    val label: String? = null,
    val sourceOffsetFrames: Int = 0,
    val sourceLoopFrames: Int = 0,
    val sourceSpanFrames: Int = 0,
    val selectionTransform: Affine2D = Affine2D(),
) {
    fun validate(): ActorPlacement = apply {
        ActorDocumentValidation.requireId(placementId, "placement ID")
        ActorDocumentValidation.requireId(projectId, "project ID")
        ActorDocumentValidation.requireId(performanceId, "performance ID")
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported placement schema $schemaVersion" }
        require(startFrame >= 0 && durationFrames > 0 && repeatCount > 0) { "Invalid placement timing" }
        require(sourceOffsetFrames >= 0 && sourceLoopFrames >= 0 && sourceSpanFrames >= 0) {
            "Invalid placement source window"
        }
        require(
            listOf(
                transform.x,
                transform.y,
                transform.rotationDegrees,
                transform.scaleX,
                transform.scaleY,
            ).all(Float::isFinite),
        ) {
            "Invalid placement transform"
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * What a saved-to-library item actually is. ACTOR/PERFORMANCE/BUNDLE are this app's own
 * structured formats (a rig, a motion, or a multi-item bundle of them, which no screen creates
 * yet); AUDIO/BACKGROUND_IMAGE/FRAME/PROJECT wrap
 * a single raw file instead (see [LibraryItemManifest.payloadFileName]) so common file types don't
 * need a bespoke JSON shape just to sit in the same library. PACK is a third shape: a named,
 * tagged collection of several stamp-ready raster drawings (a mouth pack, a hand-gesture pack --
 * see the pack manifest), always consumed as an independent copy pasted
 * onto the current frame. Actor setup may also copy a chosen drawing into revision-owned
 * artwork, align its pivot and optionally make it an expression variant. Performance keys
 * select those actor-owned variants, never live pack entries: editing/deleting the original
 * pack cannot change a saved actor. VIDEO is deliberately absent: source videos are large and
 * the app never re-encodes them, so they are never copied into the library.
 */
enum class LibraryItemKind { ACTOR, PERFORMANCE, BUNDLE, AUDIO, BACKGROUND_IMAGE, FRAME, PROJECT, PACK, PALETTE }

/**
 * Where a library item physically lives and who can see it. PROJECT-scoped
 * items are stored inside that project's own working directory (`projects/<id>/vault/`), so they
 * automatically ride along for free in every `.kframe` export/import --
 * the `.kframe` archiver already zips the whole project directory, so a
 * project-scoped item stays with the project file when it's shared with no archiver change
 * needed. PERSONAL items live in the existing shared `library/` root and are available across
 * every one of the user's projects.
 */
enum class LibraryScope { PROJECT, PERSONAL }

@Serializable
data class LibraryItemManifest(
    val schemaVersion: Int = SCHEMA_VERSION,
    val libraryItemId: String,
    val kind: LibraryItemKind,
    val displayName: String,
    val sourceActorId: String? = null,
    val sourcePerformanceId: String? = null,
    /** The project this item was captured from, for provenance -- never required for import,
     *  unlike [sourceActorId]/[sourcePerformanceId]. Also the physical storage location when
     *  [scope] is [LibraryScope.PROJECT]. */
    val sourceProjectId: String? = null,
    /** Defaults to PERSONAL so every manifest written before this field existed (every item from
     *  the first Library pass) still deserializes as exactly what it always was: a personal,
     *  cross-project item. */
    val scope: LibraryScope = LibraryScope.PERSONAL,
    /** Relative filename under this item's own `payload/` directory, for the four "raw asset"
     *  kinds in [RAW_ASSET_KINDS]. Null
     *  for ACTOR/PERFORMANCE/BUNDLE, which keep their existing structured JSON payload files
     *  (actor.json/performance.json/revision.json) instead. */
    val payloadFileName: String? = null,
    /** Free-form categorization layered on top of [kind] -- e.g. "trending", "sfx", "meme" --
     *  so search can narrow within a kind, not just filter by it. */
    val tags: Set<String> = emptySet(),
    val license: String? = null,
    val attribution: String? = null,
    val createdAtEpochMs: Long,
) {
    fun validate(): LibraryItemManifest = apply {
        ActorDocumentValidation.requireId(libraryItemId, "library item ID")
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported library schema $schemaVersion" }
        require(displayName.isNotBlank() && displayName.length <= 60) {
            "Library item name must contain 1–60 characters"
        }
        require(createdAtEpochMs >= 0) { "Invalid library timestamp" }
        require(kind != LibraryItemKind.ACTOR || sourceActorId != null) { "Actor library item needs an actor" }
        require(kind != LibraryItemKind.PERFORMANCE || sourcePerformanceId != null) {
            "Performance library item needs a performance"
        }
        require(kind !in RAW_ASSET_KINDS || payloadFileName != null) { "This library item kind needs a payload file" }
        require(
            payloadFileName == null ||
                (
                    payloadFileName != "." && payloadFileName != ".." &&
                        payloadFileName.none {
                            it == '/' || it == '\\' || it == ':'
                        }
                    ),
        ) { "Invalid library payload filename" }
        require(scope != LibraryScope.PROJECT || sourceProjectId != null) {
            "A project-scoped library item needs a project id"
        }
        require(tags.all { it.isNotBlank() && it.length <= 24 }) { "Invalid library item tag" }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        val RAW_ASSET_KINDS =
            setOf(
                LibraryItemKind.AUDIO,
                LibraryItemKind.BACKGROUND_IMAGE,
                LibraryItemKind.FRAME,
                LibraryItemKind.PROJECT,
                LibraryItemKind.PALETTE,
            )
    }
}

object ActorDocumentValidation {
    fun requireId(value: String, label: String) = StorageIds.requireValid(value, label)
}
