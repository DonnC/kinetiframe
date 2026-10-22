package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorManifest
import com.donnclab.kinetiframe.actors.rig.model.SceneGraph

/** Immutable semantic roles make saved performances portable across actor revisions. */
object ActorTemplateContract {
    private val humanRoles = setOf(
        "torso",
        "head",
        "left_upper_arm",
        "left_forearm",
        "left_hand",
        "right_upper_arm",
        "right_forearm",
        "right_hand",
        "left_thigh", "left_shin", "left_foot", "right_thigh", "right_shin", "right_foot",
    )

    /** Matches the parts [com.donnclab.kinetiframe.actors.rig.model.ActorTemplates] builds for
     *  [com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.ANIMAL] -- spine/neck/head/tail plus
     *  three joints per leg, one leg per corner (back/front x left/right). */
    private val animalRoles =
        setOf("spine", "neck", "head", "tail") + listOf("back_left", "back_right", "front_left", "front_right")
            .flatMap { corner -> listOf("${corner}_upper", "${corner}_lower", "${corner}_paw") }

    fun validate(templateId: String, graph: SceneGraph) {
        require(templateId in ActorManifest.VALID_TEMPLATE_IDS) { "Unsupported actor template '$templateId'" }
        when (templateId) {
            ActorManifest.HUMAN_TEMPLATE_ID -> {
                require(graph.bones.keys.containsAll(humanRoles)) { "Human actor is missing required structural parts" }
                require(graph.rootBoneIds == listOf("torso")) { "Human actor root must remain torso" }
            }

            ActorManifest.ANIMAL_TEMPLATE_ID -> {
                require(graph.bones.keys.containsAll(animalRoles)) {
                    "Animal actor is missing required structural parts"
                }
                require(graph.rootBoneIds == listOf("spine")) { "Animal actor root must remain spine" }
            }
            // Custom rigs are built bone-by-bone with no fixed role vocabulary -- only the
            // generic structural checks below apply, so a saved motion recipe simply can't target
            // roles that were never promised to exist.
        }
        graph.bones.forEach { (id, bone) ->
            require(id == bone.id) { "Part IDs cannot be remapped" }
            require(bone.length.isFinite() && bone.length > 0f) { "Invalid part length" }
        }
    }
}
