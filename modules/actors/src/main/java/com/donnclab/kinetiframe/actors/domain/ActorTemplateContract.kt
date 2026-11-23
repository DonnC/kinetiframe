package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.rig.model.SkeletonTemplates
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/**
 * Immutable semantic roles make saved performances portable across actor revisions. Each
 * skeleton template declares its own roles and root; every template gets the same checks.
 */
object ActorTemplateContract {
    fun validate(templateId: String, graph: SceneGraph) {
        val template = SkeletonTemplates.byId(templateId)
        require(graph.bones.keys.containsAll(template.roles)) {
            "${template.defaultName} actor is missing required structural parts"
        }
        template.rootId?.let { root ->
            require(graph.rootBoneIds == listOf(root)) { "${template.defaultName} actor root must remain $root" }
        }
        graph.bones.forEach { (id, bone) ->
            require(id == bone.id) { "Part IDs cannot be remapped" }
            require(bone.length.isFinite() && bone.length > 0f) { "Invalid part length" }
        }
    }
}
