package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorRevision
import com.donnclab.kinetiframe.actors.data.PerformanceDocument

/** Rejects motion data that refers to parts absent from the pinned actor revision. */
object PerformanceCompatibility {
    fun validate(document: PerformanceDocument, revision: ActorRevision) {
        require(document.actorId == revision.actorId) { "Performance belongs to a different actor" }
        require(document.actorRevisionId == revision.revisionId) { "Performance must use its pinned actor revision" }
        val parts = revision.sceneGraph.bones.keys
        document.artworkKeys.forEach { key ->
            key.selections.forEach { (part, variant) ->
                require(
                    part in parts &&
                        (variant.isEmpty() || variant in revision.sceneGraph.bones.getValue(part).artworkVariants),
                ) {
                    "Artwork key is incompatible with the pinned actor revision"
                }
            }
        }
        val unknownLegacy = document.poseKeys.flatMap { it.rotations.keys }.toSet() - parts
        val unknownCurves = document.rotationCurves.map { it.boneId }.toSet() - parts
        require(
            document.rootMotionCurves.all {
                it.rootId in revision.sceneGraph.rootBoneIds
            },
        ) { "Root motion must target a root part" }
        require(
            document.poseKeys.flatMap {
                it.rootTranslations.keys
            }.all { it in revision.sceneGraph.rootBoneIds },
        ) { "Root motion must target a root part" }
        document.poseKeys.flatMap { it.contacts.entries }.forEach { (id, contact) ->
            require(
                com.donnclab.kinetiframe.actors.rig.math.ActorMotionPose.contactBone(revision.sceneGraph, id) == id,
            ) {
                "Contact requires a two-bone IK chain"
            }
            require(contact.followBoneId == null || revision.sceneGraph.bones[contact.followBoneId]?.parentId == id) {
                "Contact orientation must target an attached part"
            }
        }
        // A saved preset snapshot is exempt from the "unknown revision ID" pattern used for the
        // rest of this check on purpose: ActorMotionPreset.missingBones already gates which rigs
        // a preset is offered on, so a mismatch here means the actor's rig itself changed after
        // the preset was applied -- surface that distinctly rather than folding it into the
        // generic message below.
        val missingPresetBones = document.presetSnapshot?.recipe?.missingBones(parts).orEmpty()
        require(document.presetSnapshot?.recipe?.supportsView(revision.view) != false) {
            "Preset was authored for a different actor view"
        }
        require(
            document.presetSnapshot?.recipe?.rootMotion.orEmpty().all {
                it.boneId in revision.sceneGraph.rootBoneIds
            },
        ) { "Preset translation must target a root part" }
        require(missingPresetBones.isEmpty()) {
            "Performance's applied preset needs parts this actor revision no longer has: $missingPresetBones"
        }
        require(unknownLegacy.isEmpty() && unknownCurves.isEmpty()) {
            "Performance references parts not in this actor revision"
        }
    }
}
