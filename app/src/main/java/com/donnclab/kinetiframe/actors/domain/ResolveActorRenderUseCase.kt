package com.donnclab.kinetiframe.actors.domain

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.ActorPlacement
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.data.storage.StoreResult

/** Resolves a placement once into render input; the renderer receives no file-system handles. */
class ResolveActorRenderUseCase(
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val loadPerformance: (String, String) -> StoreResult<PerformanceDocument> = performances::load,
    private val loadRevision: (String, String) -> StoreResult<ActorRevision> = actors::loadRevision,
) {
    fun execute(placement: ActorPlacement, frame: Int, fps: Int): StoreResult<ActorRenderInput> {
        val micros = ActorPlacementTiming.elapsedMicros(placement, frame, fps)
        if (micros == null) {
            return StoreResult.Failure("Placement is not active at frame $frame")
        }
        val performance = when (val result = loadPerformance(placement.projectId, placement.performanceId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> return result
        }
        val revision = when (val result = loadRevision(performance.actorId, performance.actorRevisionId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> return result
        }
        return StoreResult.Value(
            ActorRenderInput(
                placement,
                revision,
                performance,
                PerformanceEvaluator.rotations(performance, micros, revision.sceneGraph),
                PerformanceEvaluator.pose(performance, micros, revision.sceneGraph),
            ),
        )
    }
}

data class ActorRenderInput(
    val placement: ActorPlacement,
    val revision: ActorRevision,
    val performance: PerformanceDocument,
    val rotations: Map<String, Float>,
    val posedGraph: SceneGraph = revision.sceneGraph,
)
