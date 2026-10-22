package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.domain.ActorStageFraming
import com.donnclab.kinetiframe.actors.rig.model.Transform
import com.donnclab.kinetiframe.project.model.ActorPlacementReference
import com.donnclab.kinetiframe.project.model.ProjectDocument

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
