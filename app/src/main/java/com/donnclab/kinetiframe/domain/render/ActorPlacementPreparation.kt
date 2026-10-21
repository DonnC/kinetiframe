package com.donnclab.kinetiframe.domain.render

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.domain.actor.ActorStageFraming
import com.donnclab.kinetiframe.engine.model.ActorPlacementReference
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.rigging.model.Transform

/** IO worker: validate the exact render dependencies before inserting an invisible clip. */
class ActorPlacementPreparation(
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val files: ActorFileStore,
) {
    fun prepare(
        reference: ActorPlacementReference,
        project: ProjectDocument,
        repairOnly: Boolean = false,
    ): ActorPlacementReference {
        if (reference.transform != Transform()) return reference
        val renderer = ActorFrameRenderer(actors, performances, files)
        try {
            val result = renderer.resolveAll(listOf(reference), project.id, reference.startFrame, project.fps).single()
            require(result.diagnostic == null) { result.diagnostic?.message ?: "Could not place actor" }
            val points = requireNotNull(result.render) { "Performance has no visible artwork" }.geometry.quads.flatten()
            require(points.isNotEmpty()) { "Performance has no attached artwork" }
            if (repairOnly && !ActorStageFraming.outside(points, project.stage)) return reference
            return reference.copy(transform = ActorStageFraming.fit(points, project.stage))
        } finally {
            renderer.clear()
        }
    }
}
